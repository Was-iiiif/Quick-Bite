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

    // UI elements
    private Label lblTotalOrders;
    private Label lblTotalRevenue;
    private Label lblActiveOrders;
    private Label lblRating;
    private TableView<Order> ordersTable;
    private TableView<FoodItem> menuTable;

    public RestaurantDashboardView(RestaurantAdmin admin) {
        this.admin = admin;
    }

    public void show(Stage stage) {
        currentRestaurant = restaurantDAO.getById(admin.getRestaurantId());
        if (currentRestaurant == null) {
            currentRestaurant = new Restaurant(1, "Bella Italia Trattoria", "Trattoria", "101 Little Italy Way", "+1 555-1001", 4.8, "pizza.png");
        }

        stage.setTitle("QuickBite - Restaurant Admin (" + currentRestaurant.getName() + ")");

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #F8FAFC;");

        // Top Navigation Bar
        root.setTop(createNavBar(stage));

        // Center: Stats cards + Tabs for Orders & Menu
        VBox centerContent = new VBox(16);
        centerContent.setPadding(new Insets(16));

        // Stats Row
        HBox statsBar = createStatsBar();
        centerContent.getChildren().add(statsBar);

        // TabPane for Orders vs Menu Management
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        // Tab 1: Incoming Orders
        Tab ordersTab = new Tab("📋 Incoming Orders & Fulfillment");
        ordersTab.setContent(createOrdersTabContent());

        // Tab 2: Menu Management (CRUD & JSON)
        Tab menuTab = new Tab("🍕 Menu Management (CRUD & JSON)");
        menuTab.setContent(createMenuTabContent(stage));

        tabPane.getTabs().addAll(ordersTab, menuTab);
        centerContent.getChildren().add(tabPane);

        root.setCenter(centerContent);

        refreshData();

        Scene scene = new Scene(root, 1050, 720);
        try {
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        } catch (Exception ignored) {}
        stage.setScene(scene);
        stage.show();
    }

    private HBox createNavBar(Stage stage) {
        HBox nav = new HBox(16);
        nav.setStyle("-fx-background-color: #1E293B; -fx-padding: 12px 20px; -fx-alignment: CENTER_LEFT;");

        Label brand = new Label("QuickBite Restaurant Portal");
        brand.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #FF6B00;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label userLabel = new Label(currentRestaurant.getName() + " | Admin: " + admin.getName());
        userLabel.setStyle("-fx-text-fill: #F1F5F9; -fx-font-weight: bold;");

        Label roleBadge = new Label("Restaurant Admin");
        roleBadge.setStyle("-fx-background-color: #D97706; -fx-text-fill: white; -fx-padding: 3px 8px; -fx-background-radius: 12px; -fx-font-size: 11px;");

        Button btnLogout = new Button("Logout");
        btnLogout.setStyle("-fx-background-color: #EF4444; -fx-text-fill: white; -fx-font-size: 12px;");
        btnLogout.setOnAction(e -> {
            new AuthService().logout();
            new LoginView().show(stage);
        });

        nav.getChildren().addAll(brand, spacer, userLabel, roleBadge, btnLogout);
        return nav;
    }

    private HBox createStatsBar() {
        HBox bar = new HBox(16);
        bar.setAlignment(Pos.CENTER);

        VBox c1 = createStatCard("Total Orders", "0");
        lblTotalOrders = (Label) c1.getChildren().get(1);

        VBox c2 = createStatCard("Total Revenue", "$0.00");
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

        TableColumn<Order, String> colTotal = new TableColumn<>("Total");
        colTotal.setCellValueFactory(data -> new SimpleStringProperty(String.format("$%.2f", data.getValue().getTotalAmount())));
        colTotal.setMaxWidth(100);

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

        TableColumn<FoodItem, String> colPrice = new TableColumn<>("Price");
        colPrice.setCellValueFactory(data -> new SimpleStringProperty(String.format("$%.2f", data.getValue().getPrice())));
        colPrice.setMaxWidth(90);

        TableColumn<FoodItem, String> colAvail = new TableColumn<>("Availability");
        colAvail.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().isAvailable() ? "In Stock" : "Unavailable"));
        colAvail.setMaxWidth(110);

        TableColumn<FoodItem, String> colDesc = new TableColumn<>("Description");
        colDesc.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDescription()));

        menuTable.getColumns().addAll(colId, colName, colCat, colPrice, colAvail, colDesc);
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
        TextField txtCategory = new TextField(existing != null ? existing.getCategory() : "Main");
        TextField txtPrice = new TextField(existing != null ? String.valueOf(existing.getPrice()) : "9.99");
        TextArea txtDesc = new TextArea(existing != null ? existing.getDescription() : "");
        txtDesc.setPrefRowCount(3);
        CheckBox chkAvail = new CheckBox("Available in stock");
        chkAvail.setSelected(existing == null || existing.isAvailable());

        // --- Image section ---
        final String[] selectedImagePath = { (existing != null ? existing.getImageUrl() : null) };

        ImageView preview = new ImageView();
        preview.setFitWidth(340);
        preview.setFitHeight(160);
        preview.setPreserveRatio(true);
        preview.setStyle("-fx-background-color: #F1F5F9;");
        loadPreviewImage(preview, selectedImagePath[0]);

        Button btnChooseImg = new Button("🖼 Choose Image…");
        btnChooseImg.setMaxWidth(Double.MAX_VALUE);
        btnChooseImg.setStyle("-fx-background-color: #F1F5F9; -fx-text-fill: #475569; -fx-font-weight: bold; -fx-padding: 8px; -fx-border-color: #CBD5E1; -fx-border-radius: 6px; -fx-background-radius: 6px;");

        btnChooseImg.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Select Food Image");
            fc.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp", "*.webp"));
            File chosen = fc.showOpenDialog(dialog);
            if (chosen != null) {
                // Copy image into project images/ folder so path stays stable
                String saved = copyImageToImagesDir(chosen);
                selectedImagePath[0] = saved != null ? saved : chosen.getAbsolutePath();
                loadPreviewImage(preview, selectedImagePath[0]);
            }
        });

        Button btnSave = new Button("Save Dish");
        btnSave.setMaxWidth(Double.MAX_VALUE);
        btnSave.setStyle("-fx-background-color: #FF6B00; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10px;");

        btnSave.setOnAction(e -> {
            try {
                double price = Double.parseDouble(txtPrice.getText().trim());
                String imgPath = selectedImagePath[0] != null ? selectedImagePath[0] : "food.png";
                if (existing == null) {
                    FoodItem newItem = new FoodItem(0, currentRestaurant.getId(), txtName.getText().trim(),
                            txtDesc.getText().trim(), txtCategory.getText().trim(), price, chkAvail.isSelected(), imgPath);
                    menuService.addFoodItem(newItem);
                } else {
                    existing.setName(txtName.getText().trim());
                    existing.setCategory(txtCategory.getText().trim());
                    existing.setPrice(price);
                    existing.setDescription(txtDesc.getText().trim());
                    existing.setAvailable(chkAvail.isSelected());
                    existing.setImageUrl(imgPath);
                    menuService.updateFoodItem(existing);
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
                new Label("Price ($):"), txtPrice,
                new Label("Description:"), txtDesc,
                chkAvail,
                new Label("Food Image:"),
                preview,
                btnChooseImg,
                btnSave
        );

        ScrollPane scroll = new ScrollPane(form);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: white; -fx-background: white;");

        Scene scene = new Scene(scroll, 400, 600);
        try {
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        } catch (Exception ignored) {}
        dialog.setScene(scene);
        dialog.show();
    }

    /** Loads an image into the preview ImageView, showing a grey placeholder if path is null/missing. */
    private void loadPreviewImage(ImageView view, String path) {
        if (path == null || path.isBlank() || path.equals("food.png")) {
            view.setImage(null);
            view.setStyle("-fx-background-color: #E2E8F0;");
            return;
        }
        try {
            File f = new File(path);
            String uri = f.exists() ? f.toURI().toString() : path;
            Image img = new Image(uri, true);
            view.setImage(img);
            view.setStyle("");
        } catch (Exception ex) {
            view.setImage(null);
            view.setStyle("-fx-background-color: #E2E8F0;");
        }
    }

    /** Copies the chosen file to the project-local images/ folder and returns the new absolute path. */
    private String copyImageToImagesDir(File source) {
        try {
            Path imagesDir = Paths.get(System.getProperty("user.dir"), "images");
            Files.createDirectories(imagesDir);
            Path dest = imagesDir.resolve(source.getName());
            Files.copy(source.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
            return dest.toAbsolutePath().toString();
        } catch (IOException ex) {
            return null;
        }
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
        // Refresh orders table
        List<Order> orders = orderService.getRestaurantOrders(currentRestaurant.getId());
        ordersTable.getItems().setAll(orders);

        // Refresh menu table
        List<FoodItem> items = menuService.getFoodItems(currentRestaurant.getId());
        menuTable.getItems().setAll(items);

        // Refresh stats
        Map<String, Object> stats = orderService.getRestaurantStatistics(currentRestaurant.getId());
        lblTotalOrders.setText(String.valueOf(stats.getOrDefault("total_orders", 0)));
        lblTotalRevenue.setText(String.format("$%.2f", (Double) stats.getOrDefault("total_revenue", 0.0)));
        lblActiveOrders.setText(String.valueOf(stats.getOrDefault("active_orders", 0)));

        Restaurant freshRest = restaurantDAO.getById(currentRestaurant.getId());
        if (freshRest != null) {
            lblRating.setText(String.format("%.1f ★", freshRest.getRating()));
        }
    }
}
