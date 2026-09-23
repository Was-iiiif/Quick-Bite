package com.quickbite.controller;

import com.quickbite.concurrency.DeliveryDriverPool;
import com.quickbite.dao.RestaurantDAO;
import com.quickbite.model.*;
import com.quickbite.service.AuthService;
import com.quickbite.service.MenuService;
import com.quickbite.service.OrderService;
import com.quickbite.util.AlertUtil;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;

/**
 * Modern JavaFX Restaurant Admin Dashboard View.
 * Handles incoming order processing, lifecycle stage advancement (Accept -> Preparing -> Ready),
 * menu management with full CRUD and availability toggling, revenue statistics,
 * and JSON menu export/import.
 */
public class RestaurantDashboardView {
    private final RestaurantAdmin admin;
    private final RestaurantDAO restaurantDAO = new RestaurantDAO();
    private final MenuService menuService = new MenuService();
    private final OrderService orderService = new OrderService();
    private final DeliveryDriverPool driverPool = DeliveryDriverPool.getInstance();

    private Restaurant currentRestaurant;
    private Stage mainStage;

    // UI elements
    private Label lblTotalOrders;
    private Label lblTotalRevenue;
    private Label lblActiveOrders;
    private Label lblRating;
    private TableView<Order> ordersTable;
    private TableView<FoodItem> menuTable;
    private TableView<Restaurant> registeredTable;
    private ComboBox<Restaurant> cmbRestaurants;
    private Label userLabel;

    public RestaurantDashboardView(RestaurantAdmin admin) {
        this.admin = admin;
    }

    public void show(Stage stage) {
        this.mainStage = stage;
        List<Restaurant> allRests = restaurantDAO.getAll();
        currentRestaurant = restaurantDAO.getById(admin.getRestaurantId());
        if (currentRestaurant == null && !allRests.isEmpty()) {
            currentRestaurant = allRests.get(0);
            admin.setRestaurantId(currentRestaurant.getId());
        }
        if (currentRestaurant == null) {
            currentRestaurant = new Restaurant(1, "KFC Bangladesh", "Crispy Fried Chicken & Burgers", "Gulshan-1, Dhaka", "+880 1711-000000", 5.0, "kfc.png");
        }

        stage.setTitle("QuickBite - Restaurant Admin (" + currentRestaurant.getName() + ")");

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #F8FAFC;");

        // Top Navigation Bar
        root.setTop(createNavBar(stage));

        // Center: Stats cards + Tabs for Orders, Menu & Registered Outlets
        VBox centerContent = new VBox(16);
        centerContent.setPadding(new Insets(16));

        // Stats Row
        HBox statsBar = createStatsBar();
        centerContent.getChildren().add(statsBar);

        // TabPane for Orders vs Menu vs Registered Restaurants
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        // Tab 1: Incoming Orders
        Tab ordersTab = new Tab("📋 Incoming Orders & Fulfillment");
        ordersTab.setContent(createOrdersTabContent());

        // Tab 2: Menu Management (CRUD & JSON)
        Tab menuTab = new Tab("🍕 Menu Management (CRUD & JSON)");
        menuTab.setContent(createMenuTabContent(stage));

        // Tab 3: Registered Restaurants (Sync with Customer Dashboard)
        Tab registeredTab = new Tab("🏪 Registered Outlets (Customer Marketplace)");
        registeredTab.setContent(createRegisteredRestaurantsTabContent(stage));

        tabPane.getTabs().addAll(ordersTab, menuTab, registeredTab);
        centerContent.getChildren().add(tabPane);

        root.setCenter(centerContent);

        refreshData();

        Scene scene = new Scene(root, 1100, 750);
        try {
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        } catch (Exception ignored) {}
        stage.setScene(scene);
        stage.show();
    }

    private HBox createNavBar(Stage stage) {
        HBox nav = new HBox(12);
        nav.setStyle("-fx-background-color: #1E293B; -fx-padding: 12px 20px; -fx-alignment: CENTER_LEFT;");

        Label brand = new Label("QuickBite Restaurant Portal");
        brand.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #FF6B00;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label switchLbl = new Label("Active Outlet:");
        switchLbl.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 11px; -fx-font-weight: bold;");

        cmbRestaurants = new ComboBox<>();
        cmbRestaurants.setStyle("-fx-background-color: #334155; -fx-text-fill: white; -fx-font-size: 11px;");
        refreshRestaurantCombo();
        cmbRestaurants.setOnAction(e -> {
            Restaurant sel = cmbRestaurants.getValue();
            if (sel != null && (currentRestaurant == null || sel.getId() != currentRestaurant.getId())) {
                currentRestaurant = sel;
                admin.setRestaurantId(sel.getId());
                stage.setTitle("QuickBite - Restaurant Admin (" + currentRestaurant.getName() + ")");
                if (userLabel != null) userLabel.setText(currentRestaurant.getName() + " | Admin: " + admin.getName());
                refreshData();
            }
        });

        Button btnRegisterNav = new Button("➕ Register Restaurant");
        btnRegisterNav.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 6px 10px; -fx-background-radius: 6px; -fx-cursor: hand;");
        btnRegisterNav.setOnAction(e -> openRegisterRestaurantDialog(stage, null));

        userLabel = new Label(currentRestaurant.getName() + " | Admin: " + admin.getName());
        userLabel.setStyle("-fx-text-fill: #F1F5F9; -fx-font-weight: bold; -fx-font-size: 12px;");

        Label roleBadge = new Label("Restaurant Admin");
        roleBadge.setStyle("-fx-background-color: #D97706; -fx-text-fill: white; -fx-padding: 3px 8px; -fx-background-radius: 12px; -fx-font-size: 11px;");

        Button btnLogout = new Button("Logout");
        btnLogout.setStyle("-fx-background-color: #EF4444; -fx-text-fill: white; -fx-font-size: 12px; -fx-cursor: hand;");
        btnLogout.setOnAction(e -> {
            new AuthService().logout();
            new LoginView().show(stage);
        });

        nav.getChildren().addAll(brand, spacer, switchLbl, cmbRestaurants, btnRegisterNav, userLabel, roleBadge, btnLogout);
        return nav;
    }

    private HBox createStatsBar() {
        HBox bar = new HBox(16);
        bar.setAlignment(Pos.CENTER);

        VBox c1 = createStatCard("Total Orders", "0");
        lblTotalOrders = (Label) c1.getChildren().get(1);

        VBox c2 = createStatCard("Total Revenue", "BDT 0.00");
        lblTotalRevenue = (Label) c2.getChildren().get(1);
        lblTotalRevenue.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #10B981;");

        VBox c3 = createStatCard("Active Orders", "0");
        lblActiveOrders = (Label) c3.getChildren().get(1);
        lblActiveOrders.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #F59E0B;");

        VBox c4 = createStatCard("Store Rating", "5.0 ★");
        lblRating = (Label) c4.getChildren().get(1);
        lblRating.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #FF6B00;");

        bar.getChildren().addAll(c1, c2, c3, c4);
        return bar;
    }

    private VBox createStatCard(String title, String initialVal) {
        VBox card = new VBox(4);
        card.setPadding(new Insets(14));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 8px; -fx-border-color: #E2E8F0; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.04), 4, 0, 0, 1);");
        HBox.setHgrow(card, Priority.ALWAYS);

        Label tLbl = new Label(title);
        tLbl.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px; -fx-font-weight: bold;");

        Label vLbl = new Label(initialVal);
        vLbl.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");

        card.getChildren().addAll(tLbl, vLbl);
        return card;
    }

    private VBox createOrdersTabContent() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(16, 0, 0, 0));

        ordersTable = new TableView<>();
        ordersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Order, String> colId = new TableColumn<>("Order ID");
        colId.setCellValueFactory(data -> new SimpleStringProperty("#" + data.getValue().getId()));
        colId.setMaxWidth(80);

        TableColumn<Order, String> colCustomer = new TableColumn<>("Customer");
        colCustomer.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCustomerName()));

        TableColumn<Order, String> colItems = new TableColumn<>("Dishes Ordered");
        colItems.setCellValueFactory(data -> {
            StringBuilder sb = new StringBuilder();
            for (OrderItem oi : data.getValue().getItems()) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(oi.getFoodName()).append(" x").append(oi.getQuantity());
            }
            return new SimpleStringProperty(sb.toString());
        });

        TableColumn<Order, String> colTotal = new TableColumn<>("Total (BDT)");
        colTotal.setCellValueFactory(data -> new SimpleStringProperty(String.format("BDT %.2f", data.getValue().getTotalAmount())));
        colTotal.setMaxWidth(110);

        TableColumn<Order, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus()));
        colStatus.setMaxWidth(130);

        TableColumn<Order, String> colTime = new TableColumn<>("Placed At");
        colTime.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCreatedAt()));
        colTime.setMaxWidth(150);

        ordersTable.getColumns().addAll(colId, colCustomer, colItems, colTotal, colStatus, colTime);
        VBox.setVgrow(ordersTable, Priority.ALWAYS);

        // Action Buttons Row
        HBox actionsRow = new HBox(12);
        actionsRow.setAlignment(Pos.CENTER_LEFT);

        Button btnAccept = new Button("✔ Accept Order (Confirm)");
        btnAccept.setStyle("-fx-background-color: #2563EB; -fx-text-fill: white; -fx-font-weight: bold;");
        btnAccept.setOnAction(e -> advanceSelectedOrderStatus(Order.STATUS_CONFIRMED));

        Button btnPreparing = new Button("🍳 Start Preparing");
        btnPreparing.setStyle("-fx-background-color: #4F46E5; -fx-text-fill: white; -fx-font-weight: bold;");
        btnPreparing.setOnAction(e -> advanceSelectedOrderStatus(Order.STATUS_PREPARING));

        Button btnReady = new Button("📦 Ready for Delivery (Acquire Driver)");
        btnReady.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-font-weight: bold;");
        btnReady.setOnAction(e -> {
            Order sel = ordersTable.getSelectionModel().getSelectedItem();
            if (sel == null) {
                AlertUtil.showWarning("Select Order", "Please select an order from the table first.");
                return;
            }
            orderService.updateOrderStatus(sel.getId(), Order.STATUS_READY);
            // Trigger synchronized driver pool allocation
            int driverId = driverPool.acquireDriver(sel.getId());
            if (driverId != -1) {
                AlertUtil.showInfo("Driver Dispatched", "Driver #" + driverId + " has been assigned via synchronized driver pool!");
            } else {
                AlertUtil.showWarning("Driver Queue", "All delivery drivers are currently on route. Order marked READY and queued for next available driver.");
            }
            refreshData();
        });

        Button btnReject = new Button("✕ Reject / Cancel");
        btnReject.setStyle("-fx-background-color: #EF4444; -fx-text-fill: white;");
        btnReject.setOnAction(e -> advanceSelectedOrderStatus(Order.STATUS_CANCELLED));

        Button btnRefresh = new Button("🔄 Refresh");
        btnRefresh.setStyle("-fx-background-color: #F1F5F9; -fx-text-fill: #0F172A;");
        btnRefresh.setOnAction(e -> refreshData());

        actionsRow.getChildren().addAll(btnAccept, btnPreparing, btnReady, btnReject, btnRefresh);

        box.getChildren().addAll(ordersTable, actionsRow);
        return box;
    }

    private void advanceSelectedOrderStatus(String newStatus) {
        Order sel = ordersTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            AlertUtil.showWarning("Select Order", "Please select an order from the table first.");
            return;
        }
        orderService.updateOrderStatus(sel.getId(), newStatus);
        refreshData();
    }

    private VBox createMenuTabContent(Stage stage) {
        VBox box = new VBox(12);
        box.setPadding(new Insets(16, 0, 0, 0));

        menuTable = new TableView<>();
        menuTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<FoodItem, String> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getId())));
        colId.setMaxWidth(60);

        TableColumn<FoodItem, String> colName = new TableColumn<>("Item Name");
        colName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));

        TableColumn<FoodItem, String> colCat = new TableColumn<>("Category");
        colCat.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCategory()));
        colCat.setMaxWidth(120);

        TableColumn<FoodItem, String> colPrice = new TableColumn<>("Price (BDT)");
        colPrice.setCellValueFactory(data -> new SimpleStringProperty(String.format("BDT %.2f", data.getValue().getPrice())));
        colPrice.setMaxWidth(90);

        TableColumn<FoodItem, String> colAvail = new TableColumn<>("Availability");
        colAvail.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().isAvailable() ? "In Stock" : "Unavailable"));
        colAvail.setMaxWidth(110);

        TableColumn<FoodItem, String> colDesc = new TableColumn<>("Description");
        colDesc.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDescription()));

        TableColumn<FoodItem, String> colImage = new TableColumn<>("Image");
        colImage.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getImageUrl()));
        colImage.setMaxWidth(110);
        colImage.setCellFactory(col -> new TableCell<>() {
            private final ImageView thumb = new ImageView();
            private final Label lbl = new Label();
            private final HBox cellBox = new HBox(6);
            {
                thumb.setFitWidth(30);
                thumb.setFitHeight(22);
                thumb.setPreserveRatio(true);
                cellBox.setAlignment(Pos.CENTER_LEFT);
                lbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #475569;");
                cellBox.getChildren().addAll(thumb, lbl);
            }
            @Override
            protected void updateItem(String imgPath, boolean empty) {
                super.updateItem(imgPath, empty);
                if (empty || imgPath == null || imgPath.isBlank()) {
                    setGraphic(null);
                    setText(null);
                } else {
                    lbl.setText(imgPath);
                    loadThumbnail(thumb, imgPath);
                    setGraphic(cellBox);
                    setText(null);
                }
            }
        });

        menuTable.getColumns().addAll(colId, colImage, colName, colCat, colPrice, colAvail, colDesc);
        VBox.setVgrow(menuTable, Priority.ALWAYS);

        // Menu CRUD Actions Row
        HBox crudBar = new HBox(10);
        crudBar.setAlignment(Pos.CENTER_LEFT);

        Button btnAdd = new Button("+ Add Dish");
        btnAdd.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-font-weight: bold;");
        btnAdd.setOnAction(e -> openFoodDialog(stage, null));

        Button btnEdit = new Button("✎ Edit Dish");
        btnEdit.setStyle("-fx-background-color: #2563EB; -fx-text-fill: white;");
        btnEdit.setOnAction(e -> {
            FoodItem sel = menuTable.getSelectionModel().getSelectedItem();
            if (sel != null) openFoodDialog(stage, sel);
            else AlertUtil.showWarning("Select Item", "Please select a dish to edit.");
        });

        Button btnDelete = new Button("✕ Delete");
        btnDelete.setStyle("-fx-background-color: #EF4444; -fx-text-fill: white;");
        btnDelete.setOnAction(e -> {
            FoodItem sel = menuTable.getSelectionModel().getSelectedItem();
            if (sel != null) {
                if (AlertUtil.showConfirmation("Confirm Delete", "Are you sure you want to delete '" + sel.getName() + "'?")) {
                    menuService.deleteFoodItem(sel.getId());
                    refreshData();
                }
            } else {
                AlertUtil.showWarning("Select Item", "Please select a dish to delete.");
            }
        });

        Button btnToggleAvail = new Button("Toggle Stock Availability");
        btnToggleAvail.setStyle("-fx-background-color: #F59E0B; -fx-text-fill: #0F172A; -fx-font-weight: bold;");
        btnToggleAvail.setOnAction(e -> {
            FoodItem sel = menuTable.getSelectionModel().getSelectedItem();
            if (sel != null) {
                menuService.toggleAvailability(sel.getId(), !sel.isAvailable());
                refreshData();
            } else {
                AlertUtil.showWarning("Select Item", "Please select a dish to toggle.");
            }
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // JSON Import and Export Buttons (Academic Requirement)
        Button btnExportJson = new Button("📤 Export Menu (JSON)");
        btnExportJson.setStyle("-fx-background-color: #334155; -fx-text-fill: white; -fx-font-size: 11px;");
        btnExportJson.setOnAction(e -> exportMenu(stage));

        Button btnImportJson = new Button("📥 Import Menu (JSON)");
        btnImportJson.setStyle("-fx-background-color: #0F766E; -fx-text-fill: white; -fx-font-size: 11px;");
        btnImportJson.setOnAction(e -> importMenu(stage));

        crudBar.getChildren().addAll(btnAdd, btnEdit, btnDelete, btnToggleAvail, spacer, btnExportJson, btnImportJson);

        box.getChildren().addAll(menuTable, crudBar);
        return box;
    }

    private void openFoodDialog(Stage ownerStage, FoodItem existing) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initOwner(ownerStage);
        dialog.setTitle(existing == null ? "Add Food Item" : "Edit Food Item");

        VBox form = new VBox(12);
        form.setPadding(new Insets(20));
        form.setStyle("-fx-background-color: white;");

        TextField txtName = new TextField(existing != null ? existing.getName() : "");
        txtName.setPromptText("e.g. Hot & Crispy Chicken (4 pcs)");

        TextField txtCategory = new TextField(existing != null ? existing.getCategory() : "Main");
        txtCategory.setPromptText("e.g. Main, Appetizer, Sides, Beverage, Dessert");

        TextField txtPrice = new TextField(existing != null ? String.valueOf(existing.getPrice()) : "250.00");
        txtPrice.setPromptText("e.g. 250.00");

        TextArea txtDesc = new TextArea(existing != null ? existing.getDescription() : "");
        txtDesc.setPromptText("Delicious hot and crispy freshly prepared dish.");
        txtDesc.setPrefRowCount(3);

        CheckBox chkAvail = new CheckBox("Available in stock");
        chkAvail.setSelected(existing == null || existing.isAvailable());

        // --- Food Image Section ---
        Label lblImgTitle = new Label("Food Item Image:");
        lblImgTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #1E293B; -fx-font-size: 12px;");

        StackPane previewContainer = new StackPane();
        previewContainer.setPrefSize(360, 150);
        previewContainer.setMinHeight(150);
        previewContainer.setStyle("-fx-background-color: #F8FAFC; -fx-border-color: #CBD5E1; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-border-style: dashed;");

        ImageView preview = new ImageView();
        preview.setFitWidth(340);
        preview.setFitHeight(140);
        preview.setPreserveRatio(true);
        preview.setSmooth(true);

        VBox placeholderBox = new VBox(4);
        placeholderBox.setAlignment(Pos.CENTER);
        Label placeholderIcon = new Label("🍽");
        placeholderIcon.setStyle("-fx-font-size: 32px; -fx-opacity: 0.6;");
        Label placeholderText = new Label("No image selected\nBrowse an image file or choose a preset below");
        placeholderText.setStyle("-fx-font-size: 11px; -fx-text-fill: #94A3B8; -fx-text-alignment: center;");
        placeholderBox.getChildren().addAll(placeholderIcon, placeholderText);

        previewContainer.getChildren().addAll(placeholderBox, preview);

        TextField txtImageUrl = new TextField(existing != null && existing.getImageUrl() != null ? existing.getImageUrl() : "kfc.png");
        txtImageUrl.setPromptText("Image filename (e.g. kfc.png or upload a new image)");
        txtImageUrl.setStyle("-fx-font-size: 11px; -fx-padding: 6px 10px;");

        // Action buttons row for image
        HBox imgBtnRow = new HBox(8);
        imgBtnRow.setAlignment(Pos.CENTER_LEFT);

        Button btnChooseImg = new Button("📁 Browse Image File…");
        btnChooseImg.setStyle("-fx-background-color: #2563EB; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 7px 12px; -fx-background-radius: 6px; -fx-cursor: hand;");

        Button btnPresetKfc = new Button("🍗 Use KFC Brand");
        btnPresetKfc.setStyle("-fx-background-color: #F1F5F9; -fx-text-fill: #334155; -fx-font-size: 11px; -fx-border-color: #CBD5E1; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-cursor: hand;");
        btnPresetKfc.setOnAction(e -> txtImageUrl.setText("kfc.png"));

        Button btnClearImg = new Button("✕ Clear");
        btnClearImg.setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #DC2626; -fx-font-size: 11px; -fx-border-color: #FCA5A5; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-cursor: hand;");
        btnClearImg.setOnAction(e -> txtImageUrl.setText(""));

        imgBtnRow.getChildren().addAll(btnChooseImg, btnPresetKfc, btnClearImg);

        // Synchronize preview
        Runnable updatePreview = () -> {
            String path = txtImageUrl.getText().trim();
            loadPreviewImage(preview, path);
            boolean hasImage = preview.getImage() != null;
            preview.setVisible(hasImage);
            placeholderBox.setVisible(!hasImage);
        };

        txtImageUrl.textProperty().addListener((obs, oldVal, newVal) -> updatePreview.run());
        updatePreview.run();

        btnChooseImg.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Select Food Item Image");
            fc.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Image Files (*.png, *.jpg, *.jpeg, *.webp)", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp", "*.webp"));
            File chosen = fc.showOpenDialog(dialog);
            if (chosen != null) {
                String saved = copyImageToImagesDir(chosen);
                txtImageUrl.setText(saved != null ? chosen.getName() : chosen.getAbsolutePath());
                updatePreview.run();
                AlertUtil.showInfo("Image Selected", "Image '" + chosen.getName() + "' chosen and saved to images folder.");
            }
        });

        Button btnSave = new Button(existing == null ? "Save & Publish Dish" : "Update Dish");
        btnSave.setMaxWidth(Double.MAX_VALUE);
        btnSave.setStyle("-fx-background-color: #FF6B00; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10px; -fx-font-size: 13px; -fx-cursor: hand;");

        btnSave.setOnAction(e -> {
            try {
                String name = txtName.getText().trim();
                String category = txtCategory.getText().trim();
                String desc = txtDesc.getText().trim();
                String imgPath = txtImageUrl.getText().trim();
                if (imgPath.isEmpty()) {
                    imgPath = "kfc.png";
                }
                if (name.isEmpty()) {
                    AlertUtil.showWarning("Missing Name", "Please enter a dish name.");
                    return;
                }
                double price = Double.parseDouble(txtPrice.getText().trim());

                if (existing == null) {
                    FoodItem newItem = new FoodItem(0, currentRestaurant.getId(), name, desc, category, price, chkAvail.isSelected(), imgPath);
                    menuService.addFoodItem(newItem);
                    AlertUtil.showInfo("Dish Added", "'" + name + "' added successfully with image: " + imgPath);
                } else {
                    existing.setName(name);
                    existing.setCategory(category);
                    existing.setPrice(price);
                    existing.setDescription(desc);
                    existing.setAvailable(chkAvail.isSelected());
                    existing.setImageUrl(imgPath);
                    menuService.updateFoodItem(existing);
                    AlertUtil.showInfo("Dish Updated", "'" + name + "' updated successfully with image: " + imgPath);
                }
                dialog.close();
                refreshData();
            } catch (NumberFormatException ex) {
                AlertUtil.showError("Invalid Input", "Please enter a valid numeric price.");
            } catch (Exception ex) {
                AlertUtil.showError("Error", ex.getMessage());
            }
        });

        form.getChildren().addAll(
                new Label("Dish Name:"), txtName,
                new Label("Category:"), txtCategory,
                new Label("Price (BDT):"), txtPrice,
                new Label("Description:"), txtDesc,
                chkAvail,
                lblImgTitle,
                previewContainer,
                txtImageUrl,
                imgBtnRow,
                btnSave
        );

        ScrollPane scroll = new ScrollPane(form);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: white; -fx-background: white;");

        Scene scene = new Scene(scroll, 420, 660);
        try {
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        } catch (Exception ignored) {}
        dialog.setScene(scene);
        dialog.show();
    }

    /** Loads an image into the preview ImageView, supporting classpath and local files. */
    private void loadPreviewImage(ImageView view, String path) {
        if (path == null || path.isBlank()) {
            view.setImage(null);
            return;
        }
        // 1) Classpath resource (/images/<path>)
        try {
            var stream = getClass().getResourceAsStream("/images/" + path);
            if (stream != null) {
                Image img = new Image(stream, 340, 140, true, true);
                view.setImage(img);
                stream.close();
                return;
            }
        } catch (Exception ignored) {}

        // 2) Project images folder (images/<path>)
        try {
            File f = new File("images/" + path);
            if (f.exists()) {
                Image img = new Image(f.toURI().toString(), 340, 140, true, true);
                view.setImage(img);
                return;
            }
        } catch (Exception ignored) {}

        // 3) Direct file path
        try {
            File f = new File(path);
            if (f.exists()) {
                Image img = new Image(f.toURI().toString(), 340, 140, true, true);
                view.setImage(img);
                return;
            }
        } catch (Exception ignored) {}

        // 4) External URL
        if (path.startsWith("http://") || path.startsWith("https://")) {
            try {
                Image img = new Image(path, 340, 140, true, true);
                view.setImage(img);
                return;
            } catch (Exception ignored) {}
        }

        view.setImage(null);
    }

    /** Loads a compact thumbnail into an ImageView for table cells. */
    private void loadThumbnail(ImageView view, String path) {
        if (path == null || path.isBlank()) {
            view.setImage(null);
            return;
        }
        try {
            var stream = getClass().getResourceAsStream("/images/" + path);
            if (stream != null) {
                view.setImage(new Image(stream, 30, 22, true, true));
                stream.close();
                return;
            }
        } catch (Exception ignored) {}
        try {
            File f = new File("images/" + path);
            if (f.exists()) {
                view.setImage(new Image(f.toURI().toString(), 30, 22, true, true));
                return;
            }
        } catch (Exception ignored) {}
        try {
            File f = new File(path);
            if (f.exists()) {
                view.setImage(new Image(f.toURI().toString(), 30, 22, true, true));
                return;
            }
        } catch (Exception ignored) {}
        view.setImage(null);
    }

    /** Copies the chosen file to the project images/ and target/classes/images/ folders. */
    private String copyImageToImagesDir(File source) {
        try {
            Path imagesDir = Paths.get(System.getProperty("user.dir"), "images");
            Files.createDirectories(imagesDir);
            Path dest = imagesDir.resolve(source.getName());
            Files.copy(source.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);

            Path targetImagesDir = Paths.get(System.getProperty("user.dir"), "target", "classes", "images");
            if (Files.exists(targetImagesDir)) {
                Files.copy(source.toPath(), targetImagesDir.resolve(source.getName()), StandardCopyOption.REPLACE_EXISTING);
            }

            Path srcImagesDir = Paths.get(System.getProperty("user.dir"), "src", "main", "resources", "images");
            if (Files.exists(srcImagesDir)) {
                Files.copy(source.toPath(), srcImagesDir.resolve(source.getName()), StandardCopyOption.REPLACE_EXISTING);
            }

            return source.getName();
        } catch (IOException ex) {
            return source.getAbsolutePath();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // REGISTERED RESTAURANTS TAB (MARKETPLACE SYNC)
    // ─────────────────────────────────────────────────────────────────────────

    private VBox createRegisteredRestaurantsTabContent(Stage stage) {
        VBox box = new VBox(12);
        box.setPadding(new Insets(16, 0, 0, 0));

        // Callout Banner
        HBox banner = new HBox(10);
        banner.setAlignment(Pos.CENTER_LEFT);
        banner.setPadding(new Insets(10, 14, 10, 14));
        banner.setStyle("-fx-background-color: #ECFDF5; -fx-border-color: #A7F3D0; -fx-border-radius: 8px; -fx-background-radius: 8px;");

        Label badge = new Label("LIVE MARKETPLACE SYNC");
        badge.setStyle("-fx-background-color: #059669; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 3px 7px; -fx-background-radius: 4px;");

        Label desc = new Label("The Customer Dashboard will strictly and only display partner restaurants registered here. Register new outlets or modify existing listings below.");
        desc.setStyle("-fx-font-size: 11px; -fx-text-fill: #065F46; -fx-font-weight: bold;");

        banner.getChildren().addAll(badge, desc);

        // Table of registered restaurants
        registeredTable = new TableView<>();
        registeredTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Restaurant, String> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getId())));
        colId.setMaxWidth(50);

        TableColumn<Restaurant, String> colName = new TableColumn<>("Restaurant / Outlet");
        colName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));

        TableColumn<Restaurant, String> colDesc = new TableColumn<>("Cuisine / Tagline");
        colDesc.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDescription()));

        TableColumn<Restaurant, String> colAddr = new TableColumn<>("Address");
        colAddr.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getAddress()));

        TableColumn<Restaurant, String> colPhone = new TableColumn<>("Phone");
        colPhone.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPhone()));
        colPhone.setMaxWidth(130);

        TableColumn<Restaurant, String> colRating = new TableColumn<>("Rating");
        colRating.setCellValueFactory(data -> new SimpleStringProperty(String.format("%.1f ★", data.getValue().getRating())));
        colRating.setMaxWidth(80);

        TableColumn<Restaurant, String> colImage = new TableColumn<>("Banner Image");
        colImage.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getImageUrl()));
        colImage.setMaxWidth(110);

        TableColumn<Restaurant, String> colStatus = new TableColumn<>("Marketplace Status");
        colStatus.setCellValueFactory(data -> {
            boolean isActive = currentRestaurant != null && data.getValue().getId() == currentRestaurant.getId();
            return new SimpleStringProperty(isActive ? "Active (Managing)" : "Live for Customers");
        });
        colStatus.setMaxWidth(140);

        registeredTable.getColumns().addAll(colId, colName, colDesc, colAddr, colPhone, colRating, colImage, colStatus);
        VBox.setVgrow(registeredTable, Priority.ALWAYS);

        // Action Buttons Row
        HBox actionsRow = new HBox(10);
        actionsRow.setAlignment(Pos.CENTER_LEFT);

        Button btnAdd = new Button("➕ Register New Restaurant");
        btnAdd.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-font-weight: bold;");
        btnAdd.setOnAction(e -> openRegisterRestaurantDialog(stage, null));

        Button btnEdit = new Button("✎ Edit Details");
        btnEdit.setStyle("-fx-background-color: #2563EB; -fx-text-fill: white;");
        btnEdit.setOnAction(e -> {
            Restaurant sel = registeredTable.getSelectionModel().getSelectedItem();
            if (sel != null) openRegisterRestaurantDialog(stage, sel);
            else AlertUtil.showWarning("Select Restaurant", "Please select a registered restaurant to edit.");
        });

        Button btnDelete = new Button("🗑 Unregister / Delete");
        btnDelete.setStyle("-fx-background-color: #EF4444; -fx-text-fill: white;");
        btnDelete.setOnAction(e -> {
            Restaurant sel = registeredTable.getSelectionModel().getSelectedItem();
            if (sel != null) {
                if (AlertUtil.showConfirmation("Confirm Unregistration", "Are you sure you want to unregister '" + sel.getName() + "'?\nIt will be permanently removed from the Customer Dashboard marketplace.")) {
                    if (restaurantDAO.delete(sel.getId())) {
                        AlertUtil.showInfo("Unregistered", "'" + sel.getName() + "' removed from marketplace.");
                        List<Restaurant> all = restaurantDAO.getAll();
                        if (currentRestaurant != null && currentRestaurant.getId() == sel.getId()) {
                            currentRestaurant = all.isEmpty() ? null : all.get(0);
                            if (currentRestaurant != null) admin.setRestaurantId(currentRestaurant.getId());
                        }
                        refreshRestaurantCombo();
                        refreshRegisteredRestaurantsTable();
                        refreshData();
                    } else {
                        AlertUtil.showError("Error", "Could not delete restaurant.");
                    }
                }
            } else {
                AlertUtil.showWarning("Select Restaurant", "Please select a restaurant to unregister.");
            }
        });

        Button btnSetActive = new Button("✔ Select Active for Menu/Orders");
        btnSetActive.setStyle("-fx-background-color: #F59E0B; -fx-text-fill: #0F172A; -fx-font-weight: bold;");
        btnSetActive.setOnAction(e -> {
            Restaurant sel = registeredTable.getSelectionModel().getSelectedItem();
            if (sel != null) {
                currentRestaurant = sel;
                admin.setRestaurantId(sel.getId());
                stage.setTitle("QuickBite - Restaurant Admin (" + currentRestaurant.getName() + ")");
                if (userLabel != null) userLabel.setText(currentRestaurant.getName() + " | Admin: " + admin.getName());
                refreshRestaurantCombo();
                refreshRegisteredRestaurantsTable();
                refreshData();
                AlertUtil.showInfo("Active Outlet Changed", "Now managing menu and orders for '" + sel.getName() + "'.");
            } else {
                AlertUtil.showWarning("Select Restaurant", "Please select a restaurant first.");
            }
        });

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Button btnRefresh = new Button("🔄 Refresh List");
        btnRefresh.setStyle("-fx-background-color: #F1F5F9; -fx-text-fill: #0F172A;");
        btnRefresh.setOnAction(e -> {
            refreshRegisteredRestaurantsTable();
            refreshRestaurantCombo();
        });

        actionsRow.getChildren().addAll(btnAdd, btnEdit, btnDelete, btnSetActive, sp, btnRefresh);

        box.getChildren().addAll(banner, registeredTable, actionsRow);

        refreshRegisteredRestaurantsTable();
        return box;
    }

    private void openRegisterRestaurantDialog(Stage ownerStage, Restaurant existing) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initOwner(ownerStage);
        dialog.setTitle(existing == null ? "Register New Restaurant on Marketplace" : "Edit Restaurant Details");

        VBox form = new VBox(12);
        form.setPadding(new Insets(20));
        form.setStyle("-fx-background-color: white;");

        TextField txtName = new TextField(existing != null ? existing.getName() : "");
        TextField txtDesc = new TextField(existing != null ? existing.getDescription() : "");
        TextField txtAddr = new TextField(existing != null ? existing.getAddress() : "");
        TextField txtPhone = new TextField(existing != null ? existing.getPhone() : "+880 1711-000000");
        TextField txtRating = new TextField(existing != null ? String.valueOf(existing.getRating()) : "5.0");

        final String[] selectedImage = { existing != null ? existing.getImageUrl() : "kfc.png" };

        ImageView preview = new ImageView();
        preview.setFitWidth(340);
        preview.setFitHeight(140);
        preview.setPreserveRatio(true);
        loadPreviewImage(preview, selectedImage[0]);

        Button btnChooseImg = new Button("🖼 Choose Banner Image…");
        btnChooseImg.setMaxWidth(Double.MAX_VALUE);
        btnChooseImg.setStyle("-fx-background-color: #F1F5F9; -fx-text-fill: #475569; -fx-font-weight: bold; -fx-padding: 8px; -fx-border-color: #CBD5E1; -fx-border-radius: 6px; -fx-background-radius: 6px;");
        btnChooseImg.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Select Restaurant Banner Image");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.webp"));
            File chosen = fc.showOpenDialog(dialog);
            if (chosen != null) {
                String saved = copyImageToImagesDir(chosen);
                selectedImage[0] = saved != null ? chosen.getName() : chosen.getAbsolutePath();
                loadPreviewImage(preview, selectedImage[0]);
            }
        });

        Button btnSave = new Button(existing == null ? "Register & Publish Restaurant" : "Update Restaurant");
        btnSave.setMaxWidth(Double.MAX_VALUE);
        btnSave.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10px; -fx-cursor: hand;");
        btnSave.setOnAction(e -> {
            String name = txtName.getText().trim();
            String desc = txtDesc.getText().trim();
            String addr = txtAddr.getText().trim();
            String phone = txtPhone.getText().trim();
            if (name.isEmpty() || desc.isEmpty() || addr.isEmpty()) {
                AlertUtil.showWarning("Missing Details", "Please provide Restaurant Name, Cuisine/Tagline, and Street Address.");
                return;
            }
            double rating = 5.0;
            try {
                rating = Double.parseDouble(txtRating.getText().trim());
            } catch (Exception ignored) {}

            String img = (selectedImage[0] != null && !selectedImage[0].isBlank()) ? selectedImage[0] : "kfc.png";

            if (existing == null) {
                Restaurant r = new Restaurant(0, name, desc, addr, phone, rating, img);
                if (restaurantDAO.create(r)) {
                    // Create 3 starter food items for this newly registered restaurant so it has menu items
                    menuService.addFoodItem(new FoodItem(0, r.getId(), "Signature Combo Meal", "Chef special main dish with sides", "Main", 380.00, true, "kfc.png"));
                    menuService.addFoodItem(new FoodItem(0, r.getId(), "Crispy Appetizer", "Golden fried hot snack", "Appetizer", 160.00, true, "kfc.png"));
                    menuService.addFoodItem(new FoodItem(0, r.getId(), "Chilled Soft Drink", "Refreshing beverage 500ml", "Beverage", 50.00, true, "kfc.png"));

                    currentRestaurant = r;
                    admin.setRestaurantId(r.getId());
                    if (mainStage != null) {
                        mainStage.setTitle("QuickBite - Restaurant Admin (" + currentRestaurant.getName() + ")");
                    }
                    if (userLabel != null) userLabel.setText(currentRestaurant.getName() + " | Admin: " + admin.getName());
                    AlertUtil.showInfo("Registration Successful!", "'" + name + "' is now registered and published live on the Customer Dashboard!");
                } else {
                    AlertUtil.showError("Registration Failed", "Could not register restaurant in database.");
                    return;
                }
            } else {
                existing.setName(name);
                existing.setDescription(desc);
                existing.setAddress(addr);
                existing.setPhone(phone);
                existing.setRating(rating);
                existing.setImageUrl(img);
                if (restaurantDAO.update(existing)) {
                    AlertUtil.showInfo("Updated", "'" + name + "' details updated successfully.");
                } else {
                    AlertUtil.showError("Update Failed", "Could not update restaurant details.");
                    return;
                }
            }

            dialog.close();
            refreshRestaurantCombo();
            refreshRegisteredRestaurantsTable();
            refreshData();
        });

        form.getChildren().addAll(
                new Label("Restaurant Name:"), txtName,
                new Label("Cuisine / Tagline (e.g. Crispy Fried Chicken & Burgers):"), txtDesc,
                new Label("Street Address:"), txtAddr,
                new Label("Contact Phone:"), txtPhone,
                new Label("Initial Customer Rating (1.0 to 5.0):"), txtRating,
                new Label("Restaurant Banner:"), preview, btnChooseImg,
                btnSave
        );

        ScrollPane scroll = new ScrollPane(form);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: white; -fx-background: white;");

        Scene scene = new Scene(scroll, 420, 620);
        try {
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        } catch (Exception ignored) {}
        dialog.setScene(scene);
        dialog.show();
    }

    private void refreshRestaurantCombo() {
        if (cmbRestaurants == null) return;
        List<Restaurant> list = restaurantDAO.getAll();
        cmbRestaurants.getItems().setAll(list);
        if (currentRestaurant != null) {
            for (Restaurant r : list) {
                if (r.getId() == currentRestaurant.getId()) {
                    cmbRestaurants.setValue(r);
                    break;
                }
            }
        } else if (!list.isEmpty()) {
            cmbRestaurants.setValue(list.get(0));
        }
    }

    private void refreshRegisteredRestaurantsTable() {
        if (registeredTable == null) return;
        List<Restaurant> list = restaurantDAO.getAll();
        registeredTable.getItems().setAll(list);
    }

    private void exportMenu(Stage stage) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Menu to JSON File");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files (*.json)", "*.json"));
        chooser.setInitialFileName("menu_" + currentRestaurant.getId() + ".json");
        File file = chooser.showSaveDialog(stage);
        if (file != null) {
            try {
                menuService.exportMenuToJson(currentRestaurant.getId(), file);
                AlertUtil.showInfo("Export Successful", "Menu exported successfully to " + file.getName());
            } catch (Exception ex) {
                AlertUtil.showError("Export Failed", ex.getMessage());
            }
        }
    }

    private void importMenu(Stage stage) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Import Menu from JSON File");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files (*.json)", "*.json"));
        File file = chooser.showOpenDialog(stage);
        if (file != null) {
            try {
                int count = menuService.importMenuFromJson(currentRestaurant.getId(), file);
                AlertUtil.showInfo("Import Successful", "Successfully imported " + count + " food items from JSON file.");
                refreshData();
            } catch (Exception ex) {
                AlertUtil.showError("Import Failed", ex.getMessage());
            }
        }
    }

    private void refreshData() {
        if (currentRestaurant == null) {
            List<Restaurant> all = restaurantDAO.getAll();
            if (!all.isEmpty()) {
                currentRestaurant = all.get(0);
                admin.setRestaurantId(currentRestaurant.getId());
            }
        }

        if (currentRestaurant == null) {
            if (ordersTable != null) ordersTable.getItems().clear();
            if (menuTable != null) menuTable.getItems().clear();
            if (lblTotalOrders != null) lblTotalOrders.setText("0");
            if (lblTotalRevenue != null) lblTotalRevenue.setText("BDT 0.00");
            if (lblActiveOrders != null) lblActiveOrders.setText("0");
            if (lblRating != null) lblRating.setText("N/A");
            return;
        }

        // Refresh orders table
        List<Order> orders = orderService.getRestaurantOrders(currentRestaurant.getId());
        if (ordersTable != null) ordersTable.getItems().setAll(orders);

        // Refresh menu table
        List<FoodItem> items = menuService.getFoodItems(currentRestaurant.getId());
        if (menuTable != null) menuTable.getItems().setAll(items);

        // Refresh stats
        Map<String, Object> stats = orderService.getRestaurantStatistics(currentRestaurant.getId());
        if (lblTotalOrders != null) lblTotalOrders.setText(String.valueOf(stats.getOrDefault("total_orders", 0)));
        if (lblTotalRevenue != null) lblTotalRevenue.setText(String.format("BDT %.2f", (Double) stats.getOrDefault("total_revenue", 0.0)));
        if (lblActiveOrders != null) lblActiveOrders.setText(String.valueOf(stats.getOrDefault("active_orders", 0)));

        Restaurant freshRest = restaurantDAO.getById(currentRestaurant.getId());
        if (freshRest != null && lblRating != null) {
            lblRating.setText(String.format("%.1f ★", freshRest.getRating()));
        }

        refreshRestaurantCombo();
        refreshRegisteredRestaurantsTable();
    }
}
