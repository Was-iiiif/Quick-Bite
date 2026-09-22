package com.quickbite.controller;

import com.quickbite.api.SmartDeliveryService;
import com.quickbite.dao.RestaurantDAO;
import com.quickbite.dao.ReviewDAO;
import com.quickbite.model.*;
import com.quickbite.service.AuthService;
import com.quickbite.service.MenuService;
import com.quickbite.service.OrderService;
import com.quickbite.util.AlertUtil;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Rectangle;

import java.io.File;
import java.util.*;


/**
 * Modern JavaFX Customer Dashboard View.
 * Handles restaurant selection, menu browsing, category filtering, search,
 * interactive cart, checkout, live order tracking with external weather API integration,
 * order history, and review submissions.
 */
public class CustomerDashboardView {
    private final Customer customer;
    private final RestaurantDAO restaurantDAO = new RestaurantDAO();
    private final MenuService menuService = new MenuService();
    private final OrderService orderService = new OrderService();
    private final ReviewDAO reviewDAO = new ReviewDAO();
    private final SmartDeliveryService smartDeliveryService = SmartDeliveryService.getInstance();

    private final List<CartItem> cart = new ArrayList<>();
    private Restaurant selectedRestaurant;
    private String selectedCategory = "All";

    // UI Controls
    private FlowPane foodGrid;
    private VBox cartItemsBox;
    private Label lblSubtotal;
    private Label lblDeliveryFee;
    private Label lblTotal;
    private Label lblCartCount;
    private TextField searchField;
    private HBox categoryPillsBox;

    public CustomerDashboardView(Customer customer) {
        this.customer = customer;
    }

    public void show(Stage stage) {
        stage.setTitle("QuickBite - Customer Marketplace (" + customer.getName() + ")");

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #F8FAFC;");

        // Top Navigation Bar
        root.setTop(createNavBar(stage));

        // Center Content Area: Split between Restaurant/Menu Browser (Center) and Cart Sidebar (Right)
        BorderPane contentPane = new BorderPane();
        contentPane.setPadding(new Insets(16));

        // Top controls inside content: Restaurant Selector + Search + Category Pills
        VBox topControls = new VBox(12);
        topControls.setPadding(new Insets(0, 0, 16, 0));

        HBox restAndSearchRow = new HBox(16);
        restAndSearchRow.setAlignment(Pos.CENTER_LEFT);

        Label restLbl = new Label("Select Restaurant:");
        restLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

        ComboBox<Restaurant> restaurantCombo = new ComboBox<>();
        List<Restaurant> restaurants = restaurantDAO.getAll();
        restaurantCombo.getItems().addAll(restaurants);
        if (!restaurants.isEmpty()) {
            restaurantCombo.setValue(restaurants.get(0));
            selectedRestaurant = restaurants.get(0);
        }
        restaurantCombo.setStyle("-fx-pref-width: 260px;");

        restaurantCombo.setOnAction(e -> {
            selectedRestaurant = restaurantCombo.getValue();
            loadCategoryPills();
            refreshFoodMenu();
        });

        searchField = new TextField();
        searchField.setPromptText("🔍 Search dishes...");
        searchField.setStyle("-fx-pref-width: 280px;");
        searchField.textProperty().addListener((obs, oldVal, newVal) -> refreshFoodMenu());

        restAndSearchRow.getChildren().addAll(restLbl, restaurantCombo, searchField);

        categoryPillsBox = new HBox(8);
        categoryPillsBox.setAlignment(Pos.CENTER_LEFT);

        topControls.getChildren().addAll(restAndSearchRow, categoryPillsBox);
        contentPane.setTop(topControls);

        // Center: Scrollable Food Item Cards Grid
        foodGrid = new FlowPane();
        foodGrid.setHgap(16);
        foodGrid.setVgap(16);
        foodGrid.setPadding(new Insets(8));

        ScrollPane scrollPane = new ScrollPane(foodGrid);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: #F8FAFC;");
        contentPane.setCenter(scrollPane);

        // Right: Shopping Cart Sidebar
        VBox cartSidebar = createCartSidebar(stage);
        contentPane.setRight(cartSidebar);

        root.setCenter(contentPane);

        loadCategoryPills();
        refreshFoodMenu();

        Scene scene = new Scene(root, 1100, 720);
        try {
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        } catch (Exception ignored) {}
        stage.setScene(scene);
        stage.show();
    }

    private HBox createNavBar(Stage stage) {
        HBox nav = new HBox(16);
        nav.getStyleClass().add("nav-bar");
        nav.setStyle("-fx-background-color: #1E293B; -fx-padding: 12px 20px; -fx-alignment: CENTER_LEFT;");

        Label brand = new Label("QuickBite");
        brand.getStyleClass().add("brand-title");
        brand.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #FF6B00;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label userLabel = new Label("Welcome, " + customer.getName());
        userLabel.setStyle("-fx-text-fill: #F1F5F9; -fx-font-weight: bold;");

        Label roleBadge = new Label("Customer");
        roleBadge.setStyle("-fx-background-color: #0369A1; -fx-text-fill: white; -fx-padding: 3px 8px; -fx-background-radius: 12px; -fx-font-size: 11px;");

        Button btnOrders = new Button("📦 My Orders");
        btnOrders.setStyle("-fx-background-color: #334155; -fx-text-fill: white; -fx-font-size: 12px;");
        btnOrders.setOnAction(e -> showOrderHistoryModal(stage));

        Button btnLogout = new Button("Logout");
        btnLogout.setStyle("-fx-background-color: #EF4444; -fx-text-fill: white; -fx-font-size: 12px;");
        btnLogout.setOnAction(e -> {
            new AuthService().logout();
            new LoginView().show(stage);
        });

        nav.getChildren().addAll(brand, spacer, userLabel, roleBadge, btnOrders, btnLogout);
        return nav;
    }

    private void loadCategoryPills() {
        categoryPillsBox.getChildren().clear();
        if (selectedRestaurant == null) return;

        List<String> categories = new ArrayList<>();
        categories.add("All");
        categories.addAll(menuService.getCategories(selectedRestaurant.getId()));

        for (String cat : categories) {
            Button pill = new Button(cat);
            if (cat.equalsIgnoreCase(selectedCategory)) {
                pill.getStyleClass().add("pill-button-selected");
                pill.setStyle("-fx-background-color: #FF6B00; -fx-text-fill: white; -fx-background-radius: 20px; -fx-padding: 6px 14px; -fx-font-weight: bold;");
            } else {
                pill.getStyleClass().add("pill-button");
                pill.setStyle("-fx-background-color: #E2E8F0; -fx-text-fill: #475569; -fx-background-radius: 20px; -fx-padding: 6px 14px;");
            }
            pill.setOnAction(e -> {
                selectedCategory = cat;
                loadCategoryPills();
                refreshFoodMenu();
            });
            categoryPillsBox.getChildren().add(pill);
        }
    }

    private void refreshFoodMenu() {
        foodGrid.getChildren().clear();
        if (selectedRestaurant == null) return;

        List<FoodItem> items = menuService.getFoodItems(selectedRestaurant.getId());
        String query = searchField != null ? searchField.getText().toLowerCase().trim() : "";

        for (FoodItem item : items) {
            // Category filter
            if (!"All".equalsIgnoreCase(selectedCategory) && !item.getCategory().equalsIgnoreCase(selectedCategory)) {
                continue;
            }
            // Search filter
            if (!query.isEmpty() && !item.getName().toLowerCase().contains(query) && !item.getDescription().toLowerCase().contains(query)) {
                continue;
            }

            foodGrid.getChildren().add(createFoodCard(item));
        }
    }

    private VBox createFoodCard(FoodItem item) {
        VBox card = new VBox(0);
        card.setPrefWidth(260);
        card.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 10px; -fx-border-color: #E2E8F0; -fx-border-radius: 10px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 6, 0, 0, 2);");

        // --- Food image at the top of the card ---
        ImageView imgView = new ImageView();
        imgView.setFitWidth(260);
        imgView.setFitHeight(148);
        imgView.setPreserveRatio(false);

        // Rounded clip to match card corners
        Rectangle clip = new Rectangle(260, 148);
        clip.setArcWidth(10);
        clip.setArcHeight(10);
        imgView.setClip(clip);

        boolean imageLoaded = false;
        String imgPath = item.getImageUrl();
        if (imgPath != null && !imgPath.isBlank() && !imgPath.equals("food.png")) {
            try {
                File f = new File(imgPath);
                String uri = f.exists() ? f.toURI().toString() : imgPath;
                Image img = new Image(uri, 260, 148, false, true, true);
                imgView.setImage(img);
                imageLoaded = true;
            } catch (Exception ignored) {}
        }

        if (!imageLoaded) {
            // Placeholder: a styled StackPane instead of an image
            StackPane placeholder = new StackPane();
            placeholder.setPrefSize(260, 148);
            placeholder.setStyle("-fx-background-color: #F1F5F9; -fx-background-radius: 10px 10px 0 0;");
            Label icon = new Label("🍽");
            icon.setStyle("-fx-font-size: 36px;");
            placeholder.getChildren().add(icon);
            card.getChildren().add(placeholder);
        } else {
            card.getChildren().add(imgView);
        }

        // --- Content area below image ---
        VBox content = new VBox(8);
        content.setPadding(new Insets(12, 14, 14, 14));

        HBox topRow = new HBox(8);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label catBadge = new Label(item.getCategory());
        catBadge.setStyle("-fx-background-color: #F1F5F9; -fx-text-fill: #475569; -fx-padding: 2px 6px; -fx-background-radius: 6px; -fx-font-size: 10px; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label availBadge = new Label(item.isAvailable() ? "In Stock" : "Sold Out");
        availBadge.setStyle(item.isAvailable() ?
                "-fx-background-color: #D1FAE5; -fx-text-fill: #059669; -fx-padding: 2px 6px; -fx-background-radius: 6px; -fx-font-size: 10px; -fx-font-weight: bold;" :
                "-fx-background-color: #FEE2E2; -fx-text-fill: #DC2626; -fx-padding: 2px 6px; -fx-background-radius: 6px; -fx-font-size: 10px; -fx-font-weight: bold;");

        topRow.getChildren().addAll(catBadge, spacer, availBadge);

        Label nameLbl = new Label(item.getName());
        nameLbl.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");
        nameLbl.setWrapText(true);

        Label descLbl = new Label(item.getDescription());
        descLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B;");
        descLbl.setWrapText(true);
        descLbl.setPrefHeight(36);

        HBox priceAndAddRow = new HBox(8);
        priceAndAddRow.setAlignment(Pos.CENTER_LEFT);

        Label priceLbl = new Label(String.format("$%.2f", item.getPrice()));
        priceLbl.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #FF6B00;");

        Region pSpacer = new Region();
        HBox.setHgrow(pSpacer, Priority.ALWAYS);

        Button btnAdd = new Button("+ Add to Cart");
        btnAdd.setDisable(!item.isAvailable());
        btnAdd.setStyle("-fx-background-color: #FF6B00; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 6px;");
        btnAdd.setOnAction(e -> addToCart(item));

        priceAndAddRow.getChildren().addAll(priceLbl, pSpacer, btnAdd);

        content.getChildren().addAll(topRow, nameLbl, descLbl, priceAndAddRow);
        card.getChildren().add(content);
        return card;
    }


    private VBox createCartSidebar(Stage stage) {
        VBox sidebar = new VBox(12);
        sidebar.setPrefWidth(320);
        sidebar.setPadding(new Insets(16));
        sidebar.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #E2E8F0; -fx-border-width: 0 0 0 1px;");

        HBox cartHeader = new HBox(8);
        cartHeader.setAlignment(Pos.CENTER_LEFT);
        Label cartTitle = new Label("Shopping Cart");
        cartTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");

        lblCartCount = new Label("(0 items)");
        lblCartCount.setStyle("-fx-text-fill: #64748B; -fx-font-size: 12px;");
        cartHeader.getChildren().addAll(cartTitle, lblCartCount);

        cartItemsBox = new VBox(8);
        ScrollPane cartScroll = new ScrollPane(cartItemsBox);
        cartScroll.setFitToWidth(true);
        cartScroll.setStyle("-fx-background-color: transparent; -fx-background: white;");
        VBox.setVgrow(cartScroll, Priority.ALWAYS);

        // Price Summary
        VBox summaryBox = new VBox(6);
        summaryBox.setStyle("-fx-border-color: #E2E8F0; -fx-border-width: 1px 0 0 0; -fx-padding: 12px 0 0 0;");

        HBox subtotalRow = new HBox();
        lblSubtotal = new Label("$0.00");
        lblSubtotal.setStyle("-fx-font-weight: bold;");
        Region s1 = new Region(); HBox.setHgrow(s1, Priority.ALWAYS);
        subtotalRow.getChildren().addAll(new Label("Subtotal:"), s1, lblSubtotal);

        HBox feeRow = new HBox();
        lblDeliveryFee = new Label("$3.99");
        lblDeliveryFee.setStyle("-fx-font-weight: bold;");
        Region s2 = new Region(); HBox.setHgrow(s2, Priority.ALWAYS);
        feeRow.getChildren().addAll(new Label("Delivery Fee:"), s2, lblDeliveryFee);

        HBox totalRow = new HBox();
        lblTotal = new Label("$0.00");
        lblTotal.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #FF6B00;");
        Region s3 = new Region(); HBox.setHgrow(s3, Priority.ALWAYS);
        totalRow.getChildren().addAll(new Label("Total:"), s3, lblTotal);

        summaryBox.getChildren().addAll(subtotalRow, feeRow, totalRow);

        Button btnCheckout = new Button("Proceed to Checkout ➔");
        btnCheckout.setMaxWidth(Double.MAX_VALUE);
        btnCheckout.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 12px; -fx-background-radius: 6px;");
        btnCheckout.setOnAction(e -> openCheckoutDialog(stage));

        Button btnClearCart = new Button("Clear Cart");
        btnClearCart.setMaxWidth(Double.MAX_VALUE);
        btnClearCart.setStyle("-fx-background-color: #F1F5F9; -fx-text-fill: #475569; -fx-font-size: 11px;");
        btnClearCart.setOnAction(e -> {
            cart.clear();
            refreshCartDisplay();
        });

        sidebar.getChildren().addAll(cartHeader, cartScroll, summaryBox, btnCheckout, btnClearCart);
        return sidebar;
    }

    private void addToCart(FoodItem item) {
        for (CartItem ci : cart) {
            if (ci.getFoodItem().getId() == item.getId()) {
                ci.incrementQuantity();
                refreshCartDisplay();
                return;
            }
        }
        cart.add(new CartItem(item, 1));
        refreshCartDisplay();
    }

    private void refreshCartDisplay() {
        cartItemsBox.getChildren().clear();
        double subtotal = 0.0;
        int totalCount = 0;

        for (CartItem item : cart) {
            subtotal += item.getSubtotal();
            totalCount += item.getQuantity();

            HBox itemRow = new HBox(8);
            itemRow.setAlignment(Pos.CENTER_LEFT);
            itemRow.setStyle("-fx-padding: 6px; -fx-background-color: #F8FAFC; -fx-background-radius: 6px;");

            VBox info = new VBox(2);
            Label name = new Label(item.getFoodItem().getName());
            name.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
            Label price = new Label(String.format("$%.2f each", item.getUnitPrice()));
            price.setStyle("-fx-text-fill: #64748B; -fx-font-size: 10px;");
            info.getChildren().addAll(name, price);
            HBox.setHgrow(info, Priority.ALWAYS);

            Button btnMinus = new Button("-");
            btnMinus.setStyle("-fx-padding: 2px 8px; -fx-background-color: #E2E8F0;");
            btnMinus.setOnAction(e -> {
                item.decrementQuantity();
                refreshCartDisplay();
            });

            Label qtyLbl = new Label(String.valueOf(item.getQuantity()));
            qtyLbl.setStyle("-fx-font-weight: bold; -fx-padding: 0 4px;");

            Button btnPlus = new Button("+");
            btnPlus.setStyle("-fx-padding: 2px 8px; -fx-background-color: #E2E8F0;");
            btnPlus.setOnAction(e -> {
                item.incrementQuantity();
                refreshCartDisplay();
            });

            Button btnRemove = new Button("✕");
            btnRemove.setStyle("-fx-padding: 2px 6px; -fx-background-color: #FEE2E2; -fx-text-fill: #DC2626; -fx-font-size: 10px;");
            btnRemove.setOnAction(e -> {
                cart.remove(item);
                refreshCartDisplay();
            });

            itemRow.getChildren().addAll(info, btnMinus, qtyLbl, btnPlus, btnRemove);
            cartItemsBox.getChildren().add(itemRow);
        }

        double fee = cart.isEmpty() ? 0.0 : 3.99;
        double total = cart.isEmpty() ? 0.0 : subtotal + fee;

        lblCartCount.setText("(" + totalCount + " items)");
        lblSubtotal.setText(String.format("$%.2f", subtotal));
        lblDeliveryFee.setText(String.format("$%.2f", fee));
        lblTotal.setText(String.format("$%.2f", total));
    }

    private void openCheckoutDialog(Stage ownerStage) {
        if (cart.isEmpty()) {
            AlertUtil.showWarning("Empty Cart", "Please add items to your cart before proceeding to checkout.");
            return;
        }

        Stage dialog = new Stage();
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initOwner(ownerStage);
        dialog.setTitle("QuickBite - Checkout");

        VBox root = new VBox(16);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: #FFFFFF;");

        Label title = new Label("Complete Your Order");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");

        TextField txtAddress = new TextField(customer.getAddress() != null ? customer.getAddress() : "742 Evergreen Terrace");
        txtAddress.setPromptText("Delivery Address");

        ComboBox<String> paymentCombo = new ComboBox<>();
        paymentCombo.getItems().addAll("Cash on Delivery", "Credit / Debit Card", "Mobile Banking (Apple Pay / Google Pay)");
        paymentCombo.setValue("Cash on Delivery");
        paymentCombo.setMaxWidth(Double.MAX_VALUE);

        CheckBox chkSimulate = new CheckBox("Enable background asynchronous delivery simulation");
        chkSimulate.setSelected(true);
        chkSimulate.setStyle("-fx-font-size: 11px; -fx-text-fill: #0369A1;");

        double subtotal = cart.stream().mapToDouble(CartItem::getSubtotal).sum();
        double total = subtotal + 3.99;

        Label totalLbl = new Label(String.format("Total Due: $%.2f (including $3.99 delivery fee)", total));
        totalLbl.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #FF6B00;");

        Button btnConfirm = new Button("Place Order Now ➔");
        btnConfirm.setMaxWidth(Double.MAX_VALUE);
        btnConfirm.setStyle("-fx-background-color: #FF6B00; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 12px;");

        btnConfirm.setOnAction(e -> {
            try {
                Order order = orderService.placeOrder(
                        customer.getId(),
                        selectedRestaurant.getId(),
                        cart,
                        txtAddress.getText(),
                        paymentCombo.getValue(),
                        3.99,
                        chkSimulate.isSelected()
                );

                dialog.close();
                cart.clear();
                refreshCartDisplay();

                // Open Live Order Tracking View
                showOrderTrackingModal(ownerStage, order.getId());

            } catch (Exception ex) {
                AlertUtil.showError("Checkout Failed", ex.getMessage());
            }
        });

        root.getChildren().addAll(
                title,
                new Label("Confirm Delivery Address:"), txtAddress,
                new Label("Select Payment Method:"), paymentCombo,
                chkSimulate,
                totalLbl,
                btnConfirm
        );

        Scene scene = new Scene(root, 440, 380);
        try {
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        } catch (Exception ignored) {}
        dialog.setScene(scene);
        dialog.show();
    }

    /**
     * Live Order Tracking Modal with 6 visual stages and asynchronous external weather API integration.
     */
    public void showOrderTrackingModal(Stage ownerStage, int orderId) {
        Stage trackStage = new Stage();
        trackStage.initModality(Modality.NONE);
        trackStage.setTitle("QuickBite - Live Tracking for Order #" + orderId);

        VBox root = new VBox(16);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: #FFFFFF;");

        Label title = new Label("Live Order Tracking: Order #" + orderId);
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");

        // 6 Lifecycle Steps Tracker
        HBox stepsBox = new HBox(8);
        stepsBox.setAlignment(Pos.CENTER);
        stepsBox.setStyle("-fx-background-color: #F8FAFC; -fx-padding: 16px; -fx-background-radius: 8px;");

        String[] stages = {"PLACED", "CONFIRMED", "PREPARING", "READY", "OUT_FOR_DELIVERY", "DELIVERED"};
        Map<String, Label> stageLabels = new LinkedHashMap<>();

        for (int i = 0; i < stages.length; i++) {
            Label step = new Label((i + 1) + ". " + stages[i].replace("_", " "));
            step.setStyle("-fx-padding: 6px 10px; -fx-background-radius: 6px; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-color: #E2E8F0; -fx-text-fill: #64748B;");
            stageLabels.put(stages[i], step);
            stepsBox.getChildren().add(step);
            if (i < stages.length - 1) {
                Label arrow = new Label("➔");
                arrow.setStyle("-fx-text-fill: #CBD5E1;");
                stepsBox.getChildren().add(arrow);
            }
        }

        // Live Status Badge
        Label currentStatusBadge = new Label("STATUS: FETCHING...");
        currentStatusBadge.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #FF6B00;");

        // Smart Delivery Box (External Weather API Integration)
        VBox weatherBox = new VBox(6);
        weatherBox.getStyleClass().add("smart-advisory-box");
        weatherBox.setStyle("-fx-background-color: #EFF6FF; -fx-border-color: #BFDBFE; -fx-border-radius: 8px; -fx-padding: 12px;");

        Label weatherTitle = new Label("🌦 Smart Delivery Intelligence (Live Weather API)");
        weatherTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #1E40AF;");

        Label weatherDetail = new Label("Fetching real-time weather and road condition advisory via background HttpClient...");
        weatherDetail.setStyle("-fx-font-size: 11px; -fx-text-fill: #1E3A8A;");
        weatherDetail.setWrapText(true);
        weatherBox.getChildren().addAll(weatherTitle, weatherDetail);

        // Fetch external API asynchronously without freezing UI
        smartDeliveryService.fetchSmartDeliveryInfoAsync(info -> {
            Platform.runLater(() -> {
                weatherDetail.setText(String.format("Weather: %s (%.1f°C) | Estimated Delivery: ~%d mins\nAdvisory: %s",
                        info.getWeatherCondition(),
                        info.getTemperatureCelsius(),
                        info.getEstimatedMinutes(),
                        info.getWeatherAdvisory()));
            });
        });

        // Cancel Button
        Button btnCancel = new Button("Cancel Order");
        btnCancel.setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #DC2626; -fx-font-weight: bold;");
        btnCancel.setOnAction(e -> {
            try {
                if (orderService.cancelOrder(orderId)) {
                    AlertUtil.showInfo("Order Cancelled", "Your order #" + orderId + " has been cancelled.");
                    trackStage.close();
                }
            } catch (Exception ex) {
                AlertUtil.showError("Cancellation Denied", ex.getMessage());
            }
        });

        Button btnRefresh = new Button("🔄 Refresh Status");
        btnRefresh.setStyle("-fx-background-color: #F1F5F9; -fx-text-fill: #0F172A;");

        // Auto-refresh timer or updater
        Runnable updateUI = () -> {
            Order o = orderService.getOrder(orderId);
            if (o != null) {
                currentStatusBadge.setText("CURRENT STATUS: " + o.getStatus().replace("_", " "));

                // Update visual step colors
                boolean passedCurrent = false;
                for (String s : stages) {
                    Label stepLbl = stageLabels.get(s);
                    if (s.equals(o.getStatus())) {
                        stepLbl.setStyle("-fx-padding: 6px 10px; -fx-background-radius: 6px; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-color: #FF6B00; -fx-text-fill: white;");
                        passedCurrent = true;
                    } else if (!passedCurrent) {
                        stepLbl.setStyle("-fx-padding: 6px 10px; -fx-background-radius: 6px; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-color: #10B981; -fx-text-fill: white;");
                    } else {
                        stepLbl.setStyle("-fx-padding: 6px 10px; -fx-background-radius: 6px; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-color: #E2E8F0; -fx-text-fill: #64748B;");
                    }
                }

                if (Order.STATUS_CANCELLED.equals(o.getStatus())) {
                    currentStatusBadge.setText("STATUS: CANCELLED");
                    currentStatusBadge.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #DC2626;");
                }
            }
        };

        btnRefresh.setOnAction(e -> updateUI.run());
        updateUI.run();

        // Concurrency Simulator Status Listener callback
        com.quickbite.concurrency.OrderProcessingSimulator.getInstance().setStatusUpdateCallback((oid, newStatus) -> {
            if (oid == orderId) {
                Platform.runLater(updateUI);
            }
        });

        HBox btnRow = new HBox(12, btnRefresh, btnCancel);
        btnRow.setAlignment(Pos.CENTER_RIGHT);

        root.getChildren().addAll(title, stepsBox, currentStatusBadge, weatherBox, btnRow);

        Scene scene = new Scene(root, 720, 380);
        try {
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        } catch (Exception ignored) {}
        trackStage.setScene(scene);
        trackStage.show();
    }

    private void showOrderHistoryModal(Stage ownerStage) {
        Stage histStage = new Stage();
        histStage.initModality(Modality.WINDOW_MODAL);
        histStage.initOwner(ownerStage);
        histStage.setTitle("QuickBite - Order History & Reviews");

        VBox root = new VBox(16);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #F8FAFC;");

        Label title = new Label("Past Orders & Reviews");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");

        VBox ordersList = new VBox(10);
        ScrollPane scroll = new ScrollPane(ordersList);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: #F8FAFC;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        List<Order> orders = orderService.getCustomerOrders(customer.getId());

        if (orders.isEmpty()) {
            ordersList.getChildren().add(new Label("You have not placed any orders yet."));
        } else {
            for (Order o : orders) {
                VBox card = new VBox(8);
                card.setStyle("-fx-background-color: white; -fx-background-radius: 8px; -fx-padding: 14px; -fx-border-color: #E2E8F0;");

                HBox header = new HBox(8);
                header.setAlignment(Pos.CENTER_LEFT);
                Label oIdLbl = new Label("Order #" + o.getId() + " - " + o.getRestaurantName());
                oIdLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

                Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);

                Label statusBadge = new Label(o.getStatus());
                statusBadge.setStyle("-fx-padding: 3px 8px; -fx-background-radius: 6px; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-color: #E2E8F0;");
                header.getChildren().addAll(oIdLbl, sp, statusBadge);

                Label dateLbl = new Label("Placed on: " + o.getCreatedAt() + " | Total: " + String.format("$%.2f", o.getTotalAmount()));
                dateLbl.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px;");

                // Line items
                VBox itemsList = new VBox(2);
                for (OrderItem oi : o.getItems()) {
                    Label oiLbl = new Label("• " + oi.getFoodName() + " x" + oi.getQuantity() + " ($" + String.format("%.2f", oi.getSubtotal()) + ")");
                    oiLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #334155;");
                    itemsList.getChildren().add(oiLbl);
                }

                HBox actions = new HBox(8);
                actions.setAlignment(Pos.CENTER_RIGHT);

                Button btnTrack = new Button("Track Order");
                btnTrack.setStyle("-fx-background-color: #FF6B00; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold;");
                btnTrack.setOnAction(e -> showOrderTrackingModal(ownerStage, o.getId()));

                Button btnReview = new Button("★ Rate & Review");
                btnReview.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold;");
                btnReview.setOnAction(e -> showReviewDialog(ownerStage, o));

                actions.getChildren().addAll(btnTrack, btnReview);

                card.getChildren().addAll(header, dateLbl, itemsList, actions);
                ordersList.getChildren().add(card);
            }
        }

        root.getChildren().addAll(title, scroll);

        Scene scene = new Scene(root, 650, 520);
        try {
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        } catch (Exception ignored) {}
        histStage.setScene(scene);
        histStage.show();
    }

    private void showReviewDialog(Stage ownerStage, Order order) {
        if (reviewDAO.hasUserReviewedOrder(customer.getId(), order.getId())) {
            AlertUtil.showInfo("Already Reviewed", "You have already submitted a review for Order #" + order.getId() + ".");
            return;
        }

        Stage dialog = new Stage();
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initOwner(ownerStage);
        dialog.setTitle("QuickBite - Rate & Review");

        VBox root = new VBox(12);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: white;");

        Label title = new Label("Review Order #" + order.getId() + " from " + order.getRestaurantName());
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");

        HBox starBox = new HBox(8);
        starBox.setAlignment(Pos.CENTER_LEFT);
        Label starLbl = new Label("Rating (1-5 Stars):");
        ComboBox<Integer> ratingCombo = new ComboBox<>();
        ratingCombo.getItems().addAll(5, 4, 3, 2, 1);
        ratingCombo.setValue(5);
        starBox.getChildren().addAll(starLbl, ratingCombo);

        TextArea commentArea = new TextArea();
        commentArea.setPromptText("Write your feedback about the food quality, speed, and taste...");
        commentArea.setPrefRowCount(4);

        Button btnSubmit = new Button("Submit Review");
        btnSubmit.setMaxWidth(Double.MAX_VALUE);
        btnSubmit.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10px;");
        btnSubmit.setOnAction(e -> {
            Review r = new Review();
            r.setUserId(customer.getId());
            r.setRestaurantId(order.getRestaurantId());
            r.setOrderId(order.getId());
            r.setRating(ratingCombo.getValue());
            r.setComment(commentArea.getText());

            if (reviewDAO.create(r)) {
                AlertUtil.showInfo("Thank You!", "Your review has been submitted and restaurant rating updated.");
                dialog.close();
            } else {
                AlertUtil.showError("Error", "Could not save review.");
            }
        });

        root.getChildren().addAll(title, starBox, new Label("Comments:"), commentArea, btnSubmit);

        Scene scene = new Scene(root, 420, 320);
        try {
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        } catch (Exception ignored) {}
        dialog.setScene(scene);
        dialog.show();
    }
}
