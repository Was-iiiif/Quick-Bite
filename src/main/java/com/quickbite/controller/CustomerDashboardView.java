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
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.util.*;

/**
 * QuickBite Customer Dashboard View — faithful implementation of the Figma dark UI design.
 * Features:
 * - Left Navigation Rail (Home, Explore, Orders, Saved, Profile, Logout)
 * - Top Search & Location Bar with personalized greeting
 * - Recent Orders Carousel with status badges
 * - Category Filter Pills (Pizza, Sushi, Burgers, Thai, Salads, Desserts)
 * - "Near You" 6-card Restaurant Grid with badges, ratings, delivery times & menu inspector
 * - Right Sidebar with Live Active Order Stepper + Courier Card & Interactive Shopping Cart
 * - Real-time checkout, SQLite persistence, and Smart Delivery Weather API integration
 */
public class CustomerDashboardView {

    private final Customer customer;
    private final RestaurantDAO restaurantDAO = new RestaurantDAO();
    private final MenuService menuService = new MenuService();
    private final OrderService orderService = new OrderService();
    private final ReviewDAO reviewDAO = new ReviewDAO();
    private final SmartDeliveryService smartDeliveryService = SmartDeliveryService.getInstance();

    // Cart state
    private final List<CartItem> cart = new ArrayList<>();
    private Restaurant selectedRestaurant;
    private String selectedCategory = "All";
    private double promoDiscount = 0.0;
    private String appliedPromo = "";

    // UI dynamic components
    private VBox mainScrollContent;
    private FlowPane restaurantGrid;
    private VBox cartItemsBox;
    private Label lblSubtotal;
    private Label lblDeliveryFee;
    private Label lblServiceFee;
    private Label lblTotal;
    private Label lblCartCountBadge;
    private Label lblCartSource;
    private Button btnPlaceOrder;
    private TextField searchField;
    private HBox categoryPillsRow;

    // Active order dynamic widget
    private VBox activeOrderBox;
    private Order latestActiveOrder = null;
    private Label lblNearYouCount;

    public CustomerDashboardView(Customer customer) {
        this.customer = customer;
    }

    public void show(Stage stage) {
        stage.setTitle("QuickBite — " + customer.getName());

        // Ensure default restaurant is KFC Bangladesh or first available
        List<Restaurant> allRests = restaurantDAO.getAll();
        for (Restaurant r : allRests) {
            if ("KFC Bangladesh".equalsIgnoreCase(r.getName())) {
                selectedRestaurant = r;
                break;
            }
        }
        if (selectedRestaurant == null && !allRests.isEmpty()) {
            selectedRestaurant = allRests.get(0);
        }

        // Pre-populate cart with demo items if empty (matches Figma screenshot 3 items: Truffle Funghi x2, Margherita Classica x1)
        initDemoCartIfEmpty();

        HBox root = new HBox(0);
        root.setStyle("-fx-background-color: #0E0E10;");

        // 1. Left Nav Rail (fixed width 74px)
        VBox leftNav = buildLeftNavRail(stage);
        leftNav.setPrefWidth(74);
        leftNav.setMinWidth(74);
        leftNav.setMaxWidth(74);

        // 2. Center Content Area (Scrollable)
        VBox centerArea = buildCenterContent(stage);
        ScrollPane centerScroll = new ScrollPane(centerArea);
        centerScroll.setFitToWidth(true);
        centerScroll.setStyle("-fx-background-color: transparent; -fx-background: #111113; -fx-border-width: 0;");
        HBox.setHgrow(centerScroll, Priority.ALWAYS);

        // 3. Right Sidebar (fixed width 340px)
        VBox rightSidebar = buildRightSidebar(stage);
        rightSidebar.setPrefWidth(340);
        rightSidebar.setMinWidth(340);
        rightSidebar.setMaxWidth(340);

        root.getChildren().addAll(leftNav, centerScroll, rightSidebar);

        Scene scene = new Scene(root, 1280, 820);
        try {
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        } catch (Exception ignored) {}

        stage.setScene(scene);
        stage.setResizable(true);
        stage.show();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1. LEFT NAVIGATION RAIL
    // ─────────────────────────────────────────────────────────────────────────

    private VBox buildLeftNavRail(Stage stage) {
        VBox rail = new VBox(22);
        rail.setAlignment(Pos.TOP_CENTER);
        rail.setPadding(new Insets(24, 10, 24, 10));
        rail.setStyle("-fx-background-color: #0C0C0E; -fx-border-color: #1F1F24; -fx-border-width: 0 1px 0 0;");

        // QuickBite Logo Button
        Button logoBtn = new Button("⚡");
        logoBtn.setStyle(
                "-fx-background-color: #FF5722;" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 18px;" +
                "-fx-font-weight: bold;" +
                "-fx-min-width: 44px;" +
                "-fx-min-height: 44px;" +
                "-fx-max-width: 44px;" +
                "-fx-max-height: 44px;" +
                "-fx-background-radius: 12px;" +
                "-fx-cursor: hand;"
        );

        // Nav Items (Home, Explore, Orders, Saved, Profile)
        VBox navItems = new VBox(16);
        navItems.setAlignment(Pos.TOP_CENTER);

        VBox btnHome = navRailItem("⌂", "Home", true, e -> {
            selectedCategory = "All";
            refreshPills();
            refreshRestaurants();
        });

        VBox btnExplore = navRailItem("🧭", "Explore", false, e -> {
            showExploreDishesModal(stage);
        });

        VBox btnOrders = navRailItem("🛍", "Orders", false, e -> {
            showOrderHistoryModal(stage);
        });

        VBox btnSaved = navRailItem("🔖", "Saved", false, e -> {
            AlertUtil.showInfo("Saved Places", "You have saved 'KFC Bangladesh' to your favorites!");
        });

        VBox btnProfile = navRailItem("👤", "Profile", false, e -> {
            showProfileModal(stage);
        });

        navItems.getChildren().addAll(btnHome, btnExplore, btnOrders, btnSaved, btnProfile);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // Logout Button
        Button btnLogout = new Button("⇥");
        btnLogout.setStyle(
                "-fx-background-color: #1A1A20;" +
                "-fx-text-fill: #9CA3AF;" +
                "-fx-font-size: 16px;" +
                "-fx-min-width: 38px;" +
                "-fx-min-height: 38px;" +
                "-fx-background-radius: 19px;" +
                "-fx-cursor: hand;"
        );
        btnLogout.setTooltip(new Tooltip("Logout"));
        btnLogout.setOnAction(e -> {
            new AuthService().logout();
            new LoginView().show(stage);
        });

        // User Avatar with Online Dot
        StackPane avatarBox = new StackPane();
        Label avatar = new Label(customer.getName() != null && !customer.getName().isEmpty() ? customer.getName().substring(0, 1).toUpperCase() : "A");
        avatar.setStyle(
                "-fx-background-color: #27272A;" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 13px;" +
                "-fx-font-weight: bold;" +
                "-fx-min-width: 36px;" +
                "-fx-min-height: 36px;" +
                "-fx-background-radius: 18px;" +
                "-fx-alignment: center;"
        );
        Circle onlineDot = new Circle(4.5, Color.web("#22C55E"));
        StackPane.setAlignment(onlineDot, Pos.BOTTOM_RIGHT);
        avatarBox.getChildren().addAll(avatar, onlineDot);
        avatarBox.setCursor(Cursor.HAND);
        avatarBox.setOnMouseClicked(e -> showProfileModal(stage));

        rail.getChildren().addAll(logoBtn, navItems, spacer, btnLogout, avatarBox);
        return rail;
    }

    private VBox navRailItem(String icon, String label, boolean active, javafx.event.EventHandler<javafx.event.ActionEvent> action) {
        VBox box = new VBox(3);
        box.setAlignment(Pos.CENTER);
        box.setCursor(Cursor.HAND);
        box.setPrefWidth(52);
        box.setPadding(new Insets(6, 4, 6, 4));

        Label iconLbl = new Label(icon);
        iconLbl.setStyle("-fx-font-size: 18px; -fx-text-fill: " + (active ? "#FF5722;" : "#71717A;"));

        Label textLbl = new Label(label);
        textLbl.setStyle("-fx-font-size: 10px; -fx-font-weight: " + (active ? "bold;" : "normal;") + " -fx-text-fill: " + (active ? "#FF5722;" : "#71717A;"));

        if (active) {
            box.setStyle("-fx-background-color: #23120B; -fx-border-color: #FF5722; -fx-border-width: 1px; -fx-border-radius: 10px; -fx-background-radius: 10px;");
        } else {
            box.setStyle("-fx-background-color: transparent; -fx-background-radius: 10px;");
            box.setOnMouseEntered(e -> box.setStyle("-fx-background-color: #191920; -fx-background-radius: 10px;"));
            box.setOnMouseExited(e -> box.setStyle("-fx-background-color: transparent; -fx-background-radius: 10px;"));
        }

        box.getChildren().addAll(iconLbl, textLbl);
        box.setOnMouseClicked(e -> action.handle(new javafx.event.ActionEvent()));
        return box;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. CENTER SCROLLABLE DASHBOARD
    // ─────────────────────────────────────────────────────────────────────────

    private VBox buildCenterContent(Stage stage) {
        mainScrollContent = new VBox(22);
        mainScrollContent.setPadding(new Insets(26, 30, 36, 30));
        mainScrollContent.setStyle("-fx-background-color: #111113;");

        // Top Bar: Greeting + Search + Location + Notification
        HBox topBar = buildTopBar(stage);

        // Section 1: Recent Orders
        VBox recentOrdersSection = buildRecentOrdersSection(stage);

        // Section 2: Category Filter Pills
        categoryPillsRow = buildCategoryPillsRow();

        // Section 3: "Near you" Restaurant Grid
        VBox nearYouSection = buildNearYouSection(stage);

        mainScrollContent.getChildren().addAll(topBar, recentOrdersSection, categoryPillsRow, nearYouSection);
        return mainScrollContent;
    }

    private HBox buildTopBar(Stage stage) {
        HBox topBar = new HBox(16);
        topBar.setAlignment(Pos.CENTER_LEFT);

        // Greeting
        VBox greetBox = new VBox(2);
        Label quickBiteSub = new Label("QUICK BITE");
        quickBiteSub.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #71717A; -fx-letter-spacing: 1px;");

        HBox nameRow = new HBox(6);
        nameRow.setAlignment(Pos.BASELINE_LEFT);
        String firstName = customer.getName() != null ? customer.getName().split(" ")[0] : "Alex";
        Label heyName = new Label("Hey, " + firstName + " 👋");
        heyName.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: white;");

        Label cravingLbl = new Label("What are you craving?");
        cravingLbl.setStyle("-fx-font-size: 14px; -fx-text-fill: #71717A;");
        nameRow.getChildren().addAll(heyName, cravingLbl);
        greetBox.getChildren().addAll(quickBiteSub, nameRow);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Search Field
        searchField = new TextField();
        searchField.setPromptText("🔍  Restaurants, dishes...");
        searchField.setStyle(
                "-fx-background-color: #18181C;" +
                "-fx-text-fill: white;" +
                "-fx-prompt-text-fill: #71717A;" +
                "-fx-border-color: #27272F;" +
                "-fx-border-radius: 20px;" +
                "-fx-background-radius: 20px;" +
                "-fx-padding: 8px 16px;" +
                "-fx-pref-width: 250px;" +
                "-fx-font-size: 12px;"
        );
        searchField.textProperty().addListener((obs, oldV, newV) -> refreshRestaurants());

        // Location Pill
        HBox locPill = new HBox(6);
        locPill.setAlignment(Pos.CENTER);
        locPill.setCursor(Cursor.HAND);
        locPill.setStyle(
                "-fx-background-color: #18181C;" +
                "-fx-border-color: #27272F;" +
                "-fx-border-radius: 20px;" +
                "-fx-background-radius: 20px;" +
                "-fx-padding: 8px 14px;"
        );
        String addr = customer.getAddress() != null && !customer.getAddress().isEmpty() ? customer.getAddress() : "W 72nd St, New York";
        if (addr.length() > 20) addr = addr.substring(0, 18) + "...";
        Label locText = new Label("📍 " + addr + "  ▾");
        locText.setStyle("-fx-font-size: 12px; -fx-text-fill: #E4E4E7;");
        locPill.getChildren().add(locText);
        locPill.setOnMouseClicked(e -> showAddressChangeDialog());

        // Notification Bell Button
        Button bellBtn = new Button("🔔");
        bellBtn.setStyle(
                "-fx-background-color: #18181C;" +
                "-fx-border-color: #27272F;" +
                "-fx-border-radius: 20px;" +
                "-fx-background-radius: 20px;" +
                "-fx-font-size: 13px;" +
                "-fx-padding: 7px 11px;" +
                "-fx-cursor: hand;"
        );
        bellBtn.setOnAction(e -> AlertUtil.showInfo("Notifications", "You have 1 live active order: KFC Bangladesh (#1021) is Out for Delivery! ETA: ~20 min."));

        topBar.getChildren().addAll(greetBox, spacer, searchField, locPill, bellBtn);
        return topBar;
    }

    // ── Recent Orders ────────────────────────────────────────────────────────

    private VBox buildRecentOrdersSection(Stage stage) {
        VBox section = new VBox(12);

        HBox headerRow = new HBox();
        headerRow.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("Recent orders");
        title.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: white;");

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Label viewAll = new Label("View all");
        viewAll.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #FF5722; -fx-cursor: hand;");
        viewAll.setOnMouseClicked(e -> showOrderHistoryModal(stage));

        headerRow.getChildren().addAll(title, sp, viewAll);

        // 3 Cards matching the Figma design
        HBox cardsRow = new HBox(14);
        cardsRow.getChildren().addAll(
                buildRecentOrderCard("🍗", "KFC Bangladesh", "Hot & Crispy Chicken ×2, Zinger Burger", "BDT 25.50", "Today · 12:34 PM", stage),
                buildRecentOrderCard("🍗", "KFC Bangladesh", "Twister Wrap ×1, Crispy Tenders ×6", "BDT 13.80", "Yesterday · 7:12 PM", stage),
                buildRecentOrderCard("🍗", "KFC Bangladesh", "Hot Wings Combo, Fries, Pepsi", "BDT 11.20", "Sep 20 · 6:45 PM", stage)
        );

        section.getChildren().addAll(headerRow, cardsRow);
        return section;
    }

    private HBox buildRecentOrderCard(String emoji, String restName, String items, String price, String time, Stage stage) {
        HBox card = new HBox(12);
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle(
                "-fx-background-color: #17171C;" +
                "-fx-border-color: #23232A;" +
                "-fx-border-radius: 12px;" +
                "-fx-background-radius: 12px;" +
                "-fx-padding: 12px 14px;" +
                "-fx-cursor: hand;"
        );

        // Image / Emoji Icon Box
        StackPane iconBox = new StackPane();
        iconBox.setPrefSize(44, 44);
        iconBox.setStyle("-fx-background-color: #22222A; -fx-background-radius: 10px;");
        Label iconLbl = new Label(emoji);
        iconLbl.setStyle("-fx-font-size: 20px;");
        iconBox.getChildren().add(iconLbl);

        VBox info = new VBox(3);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label nameLbl = new Label(restName);
        nameLbl.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: white;");

        Label itemsLbl = new Label(items);
        itemsLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #9CA3AF;");
        itemsLbl.setWrapText(false);

        HBox priceTimeRow = new HBox(6);
        Label priceLbl = new Label(price);
        priceLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #E4E4E7;");

        Label timeLbl = new Label("· " + time);
        timeLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #71717A;");
        priceTimeRow.getChildren().addAll(priceLbl, timeLbl);

        info.getChildren().addAll(nameLbl, itemsLbl, priceTimeRow);

        // Status pill
        Label deliveredBadge = new Label("✓ Delivered");
        deliveredBadge.setStyle(
                "-fx-background-color: #062818;" +
                "-fx-text-fill: #22C55E;" +
                "-fx-font-size: 9px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 4px 8px;" +
                "-fx-background-radius: 6px;"
        );

        card.getChildren().addAll(iconBox, info, deliveredBadge);

        card.setOnMouseClicked(e -> {
            showReorderDialog(stage, restName, items, price);
        });

        return card;
    }

    // ── Category Filter Pills ────────────────────────────────────────────────

    private HBox buildCategoryPillsRow() {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);

        String[] cats = {"All", "Pizza", "Sushi", "Burgers", "Thai", "Salads", "Desserts"};
        String[] icons = {"+", "🍕", "🍣", "🍔", "🍜", "🥗", "🍰"};

        for (int i = 0; i < cats.length; i++) {
            final String cat = cats[i];
            final String icon = icons[i];

            Button pill = new Button((icon.equals("+") ? "+ All" : icon + " " + cat));
            pill.setCursor(Cursor.HAND);
            styleCategoryPill(pill, cat.equalsIgnoreCase(selectedCategory));

            pill.setOnAction(e -> {
                selectedCategory = cat;
                refreshPills();
                refreshRestaurants();
            });

            row.getChildren().add(pill);
        }

        return row;
    }

    private void styleCategoryPill(Button pill, boolean isSelected) {
        if (isSelected) {
            pill.setStyle(
                    "-fx-background-color: #FF5722;" +
                    "-fx-text-fill: white;" +
                    "-fx-font-size: 12px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-padding: 7px 16px;" +
                    "-fx-background-radius: 20px;"
            );
        } else {
            pill.setStyle(
                    "-fx-background-color: #191920;" +
                    "-fx-text-fill: #D4D4D8;" +
                    "-fx-font-size: 12px;" +
                    "-fx-font-weight: normal;" +
                    "-fx-padding: 7px 16px;" +
                    "-fx-border-color: #272730;" +
                    "-fx-border-radius: 20px;" +
                    "-fx-background-radius: 20px;"
            );
        }
    }

    private void refreshPills() {
        if (categoryPillsRow == null) return;
        for (javafx.scene.Node node : categoryPillsRow.getChildren()) {
            if (node instanceof Button btn) {
                String txt = btn.getText();
                boolean isMatch = (selectedCategory.equalsIgnoreCase("All") && txt.contains("All")) ||
                                  (!selectedCategory.equalsIgnoreCase("All") && txt.toLowerCase().contains(selectedCategory.toLowerCase()));
                styleCategoryPill(btn, isMatch);
            }
        }
    }

    // ── "Near you" Restaurant Grid ───────────────────────────────────────────

    private VBox buildNearYouSection(Stage stage) {
        VBox section = new VBox(14);

        HBox headerRow = new HBox(8);
        headerRow.setAlignment(Pos.BASELINE_LEFT);

        Label title = new Label("Near you");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");

        lblNearYouCount = new Label("6 places");
        lblNearYouCount.setStyle("-fx-font-size: 13px; -fx-text-fill: #71717A;");

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Button btnManage = new Button("⚙ Manage Places");
        btnManage.setStyle(
                "-fx-background-color: #1E1E26;" +
                "-fx-text-fill: #FF5722;" +
                "-fx-border-color: #38241D;" +
                "-fx-border-radius: 8px;" +
                "-fx-background-radius: 8px;" +
                "-fx-font-size: 11px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 5px 12px;" +
                "-fx-cursor: hand;"
        );
        btnManage.setOnAction(e -> showManageRestaurantsModal(stage));

        Button btnFilter = new Button("⚡ Filter");
        btnFilter.setStyle(
                "-fx-background-color: #18181C;" +
                "-fx-text-fill: #D4D4D8;" +
                "-fx-border-color: #272730;" +
                "-fx-border-radius: 8px;" +
                "-fx-background-radius: 8px;" +
                "-fx-font-size: 11px;" +
                "-fx-padding: 5px 12px;" +
                "-fx-cursor: hand;"
        );
        btnFilter.setOnAction(e -> {
            AlertUtil.showInfo("Filter Options", "Currently sorted by Top Rating & Nearest Delivery Zone.");
        });

        headerRow.getChildren().addAll(title, lblNearYouCount, sp, btnManage, btnFilter);

        restaurantGrid = new FlowPane();
        restaurantGrid.setHgap(16);
        restaurantGrid.setVgap(18);
        restaurantGrid.setAlignment(Pos.TOP_LEFT);

        refreshRestaurants();

        section.getChildren().addAll(headerRow, restaurantGrid);
        return section;
    }

    private void refreshRestaurants() {
        if (restaurantGrid == null) return;
        restaurantGrid.getChildren().clear();

        List<Restaurant> allRests = restaurantDAO.getAll();
        String query = searchField != null ? searchField.getText().toLowerCase().trim() : "";
        int displayedCount = 0;


        // Map mockup details
        Map<String, String[]> meta = new LinkedHashMap<>();
        meta.put("KFC Bangladesh", new String[]{"TOP PICK", "20% OFF", "Crispy Fried Chicken & Burgers", "★ 5.0 (2.8k)", "20–30 min", "Free", "#E4002B"});
        meta.put("Ember & Ash", new String[]{"POPULAR", "30% OFF", "Wood-fired Pizza", "★ 4.9 (1.2k)", "22–32 min", "Free", "#E25822"});
        meta.put("Shogun Omakase", new String[]{"NEW", "", "Premium Sushi", "★ 4.8 (876)", "35–45 min", "BDT 1.99", "#3B82F6"});
        meta.put("The Patty Lab", new String[]{"", "", "Craft Burgers", "★ 4.7 (2.1k)", "18–28 min", "Free", "#EAB308"});
        meta.put("Lemongrass House", new String[]{"", "", "Authentic Thai", "★ 4.6 (543)", "28–38 min", "BDT 0.99", "#10B981"});
        meta.put("Field & Fork", new String[]{"HEALTHY", "", "Garden Salads", "★ 4.5 (389)", "15–25 min", "Free", "#14B8A6"});
        meta.put("Petite Maison", new String[]{"TOP RATED", "", "French Desserts", "★ 4.9 (718)", "30–40 min", "BDT 2.49", "#EC4899"});

        for (Restaurant r : allRests) {
            String[] m = meta.get(r.getName());
            String cuisine = m != null ? m[2] : r.getDescription();

            // Filter by category
            if (!"All".equalsIgnoreCase(selectedCategory)) {
                boolean matchesCategory = false;
                if (cuisine != null && cuisine.toLowerCase().contains(selectedCategory.toLowerCase())) matchesCategory = true;
                if (r.getName().toLowerCase().contains(selectedCategory.toLowerCase())) matchesCategory = true;
                // Check if any menu items match
                List<FoodItem> items = menuService.getFoodItems(r.getId());
                for (FoodItem fi : items) {
                    if (fi.getCategory().equalsIgnoreCase(selectedCategory)) {
                        matchesCategory = true;
                        break;
                    }
                }
                if (!matchesCategory) continue;
            }

            // Filter by search query
            if (!query.isEmpty()) {
                boolean matchesSearch = r.getName().toLowerCase().contains(query) || (cuisine != null && cuisine.toLowerCase().contains(query));
                if (!matchesSearch) {
                    List<FoodItem> items = menuService.getFoodItems(r.getId());
                    for (FoodItem fi : items) {
                        if (fi.getName().toLowerCase().contains(query) || fi.getDescription().toLowerCase().contains(query)) {
                            matchesSearch = true;
                            break;
                        }
                    }
                }
                if (!matchesSearch) continue;
            }

            restaurantGrid.getChildren().add(buildRestaurantCard(r, m));
            displayedCount++;
        }

        if (lblNearYouCount != null) {
            lblNearYouCount.setText(displayedCount + (displayedCount == 1 ? " place" : " places"));
        }
    }

    private VBox buildRestaurantCard(Restaurant r, String[] m) {
        VBox card = new VBox(0);
        card.setPrefWidth(265);
        card.setStyle(
                "-fx-background-color: #17171B;" +
                "-fx-border-color: #24242C;" +
                "-fx-border-radius: 14px;" +
                "-fx-background-radius: 14px;" +
                "-fx-cursor: hand;"
        );

        // Hover effect
        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: #1C1C22; -fx-border-color: #383844; -fx-border-radius: 14px; -fx-background-radius: 14px; -fx-cursor: hand;"));
        card.setOnMouseExited(e -> card.setStyle("-fx-background-color: #17171B; -fx-border-color: #24242C; -fx-border-radius: 14px; -fx-background-radius: 14px; -fx-cursor: hand;"));

        // 1. Top Image Banner with Badges
        StackPane banner = new StackPane();
        banner.setPrefSize(265, 128);

        // Background styling / loaded image
        ImageView bannerImg = new ImageView();
        bannerImg.setFitWidth(265);
        bannerImg.setFitHeight(128);
        bannerImg.setPreserveRatio(false);
        Rectangle clip = new Rectangle(265, 128);
        clip.setArcWidth(14);
        clip.setArcHeight(14);
        bannerImg.setClip(clip);

        boolean loaded = false;
        if (r.getImageUrl() != null && !r.getImageUrl().isBlank()) {
            // 1) Try classpath resource first (src/main/resources/images/)
            try {
                var stream = getClass().getResourceAsStream("/images/" + r.getImageUrl());
                if (stream != null) {
                    Image img = new Image(stream, 265, 128, false, true);
                    bannerImg.setImage(img);
                    stream.close();
                    loaded = true;
                }
            } catch (Exception ignored) {}
            // 2) Fallback: raw file path or URL
            if (!loaded) {
                try {
                    File f = new File(r.getImageUrl());
                    String uri = f.exists() ? f.toURI().toString() : r.getImageUrl();
                    Image img = new Image(uri, 265, 128, false, true, true);
                    bannerImg.setImage(img);
                    loaded = true;
                } catch (Exception ignored) {}
            }
        }

        // Stylish gradient placeholder with themed emoji if no image file
        StackPane placeholder = new StackPane();
        placeholder.setPrefSize(265, 128);
        String colorAccent = m != null && m.length > 6 ? m[6] : "#E25822";
        placeholder.setStyle("-fx-background-color: linear-gradient(to bottom right, #24242C, " + colorAccent + "33); -fx-background-radius: 14px 14px 0 0;");
        Label icon = new Label(getRestaurantEmoji(r.getName()));
        icon.setStyle("-fx-font-size: 44px;");
        placeholder.getChildren().add(icon);

        if (loaded) banner.getChildren().add(bannerImg);
        else banner.getChildren().add(placeholder);

        // Floating Badges on Image
        HBox badgeBar = new HBox(6);
        badgeBar.setPadding(new Insets(10));
        badgeBar.setAlignment(Pos.TOP_LEFT);

        if (m != null) {
            String leftTag = m[0];
            String rightTag = m[1];

            if (!leftTag.isEmpty()) {
                Label tagLbl = new Label(leftTag);
                tagLbl.setStyle(
                        "-fx-background-color: rgba(255, 87, 34, 0.9);" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 9px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 3px 7px;" +
                        "-fx-background-radius: 6px;"
                );
                badgeBar.getChildren().add(tagLbl);
            }

            Region sp = new Region();
            HBox.setHgrow(sp, Priority.ALWAYS);
            badgeBar.getChildren().add(sp);

            if (!rightTag.isEmpty()) {
                Label rightTagLbl = new Label(rightTag);
                rightTagLbl.setStyle(
                        "-fx-background-color: rgba(220, 38, 38, 0.9);" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 9px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 3px 7px;" +
                        "-fx-background-radius: 6px;"
                );
                badgeBar.getChildren().add(rightTagLbl);
            }
        }
        banner.getChildren().add(badgeBar);

        // 2. Card Content
        VBox content = new VBox(6);
        content.setPadding(new Insets(12, 14, 14, 14));

        // Name + Rating Row
        HBox nameRow = new HBox(6);
        nameRow.setAlignment(Pos.CENTER_LEFT);

        Label nameLbl = new Label(r.getName());
        nameLbl.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: white;");

        Region sp2 = new Region();
        HBox.setHgrow(sp2, Priority.ALWAYS);

        String ratingStr = m != null ? m[3] : String.format("★ %.1f", r.getRating());
        Label ratingLbl = new Label(ratingStr);
        ratingLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #F59E0B;");

        nameRow.getChildren().addAll(nameLbl, sp2, ratingLbl);

        // Cuisine Description
        String cuisineStr = m != null ? m[2] : r.getDescription();
        Label cuisineLbl = new Label(cuisineStr);
        cuisineLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #9CA3AF;");

        // Footer info (Delivery time + Fee)
        HBox footerRow = new HBox(8);
        footerRow.setAlignment(Pos.CENTER_LEFT);
        footerRow.setPadding(new Insets(4, 0, 0, 0));

        String timeStr = m != null ? m[4] : "20–30 min";
        Label timeLbl = new Label("🕒 " + timeStr);
        timeLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #71717A;");

        String feeStr = m != null ? m[5] : "Free";
        Label feeLbl = new Label(feeStr.equalsIgnoreCase("Free") ? "✓ Free delivery" : feeStr + " delivery");
        feeLbl.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: " + (feeStr.equalsIgnoreCase("Free") ? "#22C55E;" : "#9CA3AF;"));

        footerRow.getChildren().addAll(timeLbl, feeLbl);

        content.getChildren().addAll(nameRow, cuisineLbl, footerRow);
        card.getChildren().addAll(banner, content);

        // Click on restaurant card opens its full menu inspector
        card.setOnMouseClicked(e -> {
            if (e.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                selectedRestaurant = r;
                if (lblCartSource != null) {
                    lblCartSource.setText("From " + r.getName());
                }
                showRestaurantMenuModal(r);
            }
        });

        // Right-click context menu for quick actions
        ContextMenu ctxMenu = new ContextMenu();
        ctxMenu.setStyle("-fx-background-color: #1A1A22; -fx-border-color: #2F2F3B;");
        MenuItem miMenu = new MenuItem("🍽 View Menu & Dishes");
        miMenu.setOnAction(e -> {
            selectedRestaurant = r;
            if (lblCartSource != null) lblCartSource.setText("From " + r.getName());
            showRestaurantMenuModal(r);
        });
        MenuItem miDelete = new MenuItem("🗑 Remove Restaurant");
        miDelete.setStyle("-fx-text-fill: #EF4444;");
        miDelete.setOnAction(e -> {
            if (AlertUtil.showConfirmation("Remove Restaurant", "Are you sure you want to remove '" + r.getName() + "' from QuickBite?")) {
                if (restaurantDAO.delete(r.getId())) {
                    AlertUtil.showInfo("Removed", "'" + r.getName() + "' was removed successfully.");
                    refreshRestaurants();
                } else {
                    AlertUtil.showError("Error", "Could not remove restaurant.");
                }
            }
        });
        ctxMenu.getItems().addAll(miMenu, miDelete);
        card.setOnContextMenuRequested(e -> ctxMenu.show(card, e.getScreenX(), e.getScreenY()));

        return card;
    }

    private String getRestaurantEmoji(String name) {
        if (name == null) return "🍽";
        if (name.contains("KFC")) return "🍗";
        if (name.contains("Ember") || name.contains("Italia") || name.contains("Pizza")) return "🍕";
        if (name.contains("Shogun") || name.contains("Tokyo") || name.contains("Sushi") || name.contains("Ramen")) return "🍣";
        if (name.contains("Patty") || name.contains("Burger")) return "🍔";
        if (name.contains("Lemongrass") || name.contains("Thai")) return "🍜";
        if (name.contains("Field") || name.contains("Green") || name.contains("Salad")) return "🥗";
        if (name.contains("Petite") || name.contains("Maison") || name.contains("Dessert")) return "🍰";
        return "🍽";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. RIGHT SIDEBAR — ACTIVE ORDER + CART
    // ─────────────────────────────────────────────────────────────────────────

    private VBox buildRightSidebar(Stage stage) {
        VBox sidebar = new VBox(20);
        sidebar.setPadding(new Insets(24, 20, 24, 20));
        sidebar.setStyle("-fx-background-color: #131316; -fx-border-color: #1F1F24; -fx-border-width: 0 0 0 1px;");

        // 1. ACTIVE ORDER SECTION
        activeOrderBox = buildActiveOrderWidget();

        // 2. YOUR CART SECTION
        VBox cartBox = buildCartWidget(stage);
        VBox.setVgrow(cartBox, Priority.ALWAYS);

        sidebar.getChildren().addAll(activeOrderBox, cartBox);
        return sidebar;
    }

    private VBox buildActiveOrderWidget() {
        VBox box = new VBox(12);
        box.setStyle("-fx-background-color: #17171C; -fx-border-color: #24242C; -fx-border-radius: 12px; -fx-background-radius: 12px; -fx-padding: 16px;");

        // Header
        HBox head = new HBox();
        head.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("ACTIVE ORDER");
        title.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #9CA3AF; -fx-letter-spacing: 0.5px;");

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Label timerPill = new Label("● ~18 min");
        timerPill.setStyle("-fx-background-color: #2A130A; -fx-text-fill: #FF5722; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 3px 8px; -fx-background-radius: 10px;");

        head.getChildren().addAll(title, sp, timerPill);

        // Order Summary
        HBox orderSummary = new HBox(10);
        orderSummary.setAlignment(Pos.CENTER_LEFT);

        StackPane restImg = new StackPane();
        restImg.setPrefSize(38, 38);
        restImg.setStyle("-fx-background-color: #272730; -fx-background-radius: 8px;");
        Label restEmoji = new Label("🍗");
        restEmoji.setStyle("-fx-font-size: 18px;");
        restImg.getChildren().add(restEmoji);

        VBox orderInfo = new VBox(2);
        Label orderTitle = new Label("KFC Bangladesh • #1021");
        orderTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: white;");

        Label orderSub = new Label("Hot & Crispy Chicken ×2, Zinger Burger");
        orderSub.setStyle("-fx-font-size: 10px; -fx-text-fill: #9CA3AF;");
        orderInfo.getChildren().addAll(orderTitle, orderSub);

        orderSummary.getChildren().addAll(restImg, orderInfo);

        // Vertical Stepper (4 Steps)
        VBox stepper = new VBox(8);
        stepper.setPadding(new Insets(6, 0, 6, 6));

        stepper.getChildren().addAll(
                buildStepRow("✓", "Order received", true, false),
                buildStepRow("✓", "Preparing", true, false),
                buildStepRow("◉", "Out for delivery", true, true),
                buildStepRow("○", "Delivered", false, false)
        );

        // Courier Card
        HBox courierCard = new HBox(10);
        courierCard.setAlignment(Pos.CENTER_LEFT);
        courierCard.setStyle("-fx-background-color: #1F1F26; -fx-border-color: #2B2B36; -fx-border-radius: 10px; -fx-background-radius: 10px; -fx-padding: 8px 12px;");

        StackPane courierAvatar = new StackPane();
        courierAvatar.setPrefSize(32, 32);
        courierAvatar.setStyle("-fx-background-color: #3F3F46; -fx-background-radius: 16px;");
        Label cIcon = new Label("🛵");
        cIcon.setStyle("-fx-font-size: 14px;");
        courierAvatar.getChildren().add(cIcon);

        VBox cInfo = new VBox(1);
        Label cName = new Label("Marco A.");
        cName.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: white;");
        Label cSub = new Label("★ 4.9 · Your courier");
        cSub.setStyle("-fx-font-size: 10px; -fx-text-fill: #9CA3AF;");
        cInfo.getChildren().addAll(cName, cSub);

        Region cSp = new Region();
        HBox.setHgrow(cSp, Priority.ALWAYS);

        Button btnCall = new Button("📞");
        btnCall.setStyle("-fx-background-color: #FF5722; -fx-text-fill: white; -fx-font-size: 12px; -fx-min-width: 28px; -fx-min-height: 28px; -fx-background-radius: 14px; -fx-cursor: hand;");
        btnCall.setOnAction(e -> AlertUtil.showInfo("Call Courier", "Calling Marco A. at +1 (555) 019-4821..."));

        courierCard.getChildren().addAll(courierAvatar, cInfo, cSp, btnCall);

        box.getChildren().addAll(head, orderSummary, stepper, courierCard);
        return box;
    }

    private HBox buildStepRow(String symbol, String label, boolean completed, boolean isCurrent) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);

        Label dot = new Label(symbol);
        if (isCurrent) {
            dot.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #FF5722;");
        } else if (completed) {
            dot.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #FF5722;");
        } else {
            dot.setStyle("-fx-font-size: 11px; -fx-text-fill: #52525B;");
        }

        Label text = new Label(label);
        if (isCurrent) {
            text.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #FF5722;");
        } else if (completed) {
            text.setStyle("-fx-font-size: 11px; -fx-text-fill: #E4E4E7;");
        } else {
            text.setStyle("-fx-font-size: 11px; -fx-text-fill: #52525B;");
        }

        row.getChildren().addAll(dot, text);
        return row;
    }

    // ── Cart Section ─────────────────────────────────────────────────────────

    private VBox buildCartWidget(Stage stage) {
        VBox box = new VBox(12);

        // Cart Header
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Your cart");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white;");

        lblCartCountBadge = new Label(cart.size() + " items");
        lblCartCountBadge.setStyle("-fx-background-color: #2D140D; -fx-text-fill: #FF5722; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 2px 7px; -fx-background-radius: 6px;");

        header.getChildren().addAll(title, lblCartCountBadge);

        // Cart Restaurant Source
        String sourceName = selectedRestaurant != null ? selectedRestaurant.getName() : "KFC Bangladesh";
        lblCartSource = new Label("From " + sourceName);
        lblCartSource.setStyle("-fx-font-size: 11px; -fx-text-fill: #9CA3AF;");

        // Scrollable Cart Items
        cartItemsBox = new VBox(10);
        ScrollPane cartScroll = new ScrollPane(cartItemsBox);
        cartScroll.setFitToWidth(true);
        cartScroll.setPrefHeight(170);
        cartScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-width: 0;");
        VBox.setVgrow(cartScroll, Priority.ALWAYS);

        // Price Breakdown
        VBox summary = new VBox(6);
        summary.setStyle("-fx-border-color: #22222A; -fx-border-width: 1px 0 0 0; -fx-padding: 10px 0 0 0;");

        HBox subtotalRow = new HBox();
        lblSubtotal = new Label("BDT 0.00");
        lblSubtotal.setStyle("-fx-text-fill: #E4E4E7; -fx-font-size: 12px;");
        Region s1 = new Region(); HBox.setHgrow(s1, Priority.ALWAYS);
        subtotalRow.getChildren().addAll(styledLabel("Subtotal"), s1, lblSubtotal);

        HBox deliveryRow = new HBox();
        lblDeliveryFee = new Label("Free");
        lblDeliveryFee.setStyle("-fx-text-fill: #22C55E; -fx-font-weight: bold; -fx-font-size: 12px;");
        Region s2 = new Region(); HBox.setHgrow(s2, Priority.ALWAYS);
        deliveryRow.getChildren().addAll(styledLabel("Delivery"), s2, lblDeliveryFee);

        HBox serviceRow = new HBox();
        lblServiceFee = new Label("BDT 2.50");
        lblServiceFee.setStyle("-fx-text-fill: #E4E4E7; -fx-font-size: 12px;");
        Region s3 = new Region(); HBox.setHgrow(s3, Priority.ALWAYS);
        serviceRow.getChildren().addAll(styledLabel("Service fee"), s3, lblServiceFee);

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #22222A;");

        HBox totalRow = new HBox();
        totalRow.setAlignment(Pos.CENTER_LEFT);
        Label totalTitle = new Label("Total");
        totalTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: white;");
        Region s4 = new Region(); HBox.setHgrow(s4, Priority.ALWAYS);
        lblTotal = new Label("BDT 0.00");
        lblTotal.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white;");
        totalRow.getChildren().addAll(totalTitle, s4, lblTotal);

        summary.getChildren().addAll(subtotalRow, deliveryRow, serviceRow, sep, totalRow);

        // Promo Code Box
        HBox promoBox = new HBox(8);
        promoBox.setAlignment(Pos.CENTER_LEFT);
        TextField txtPromo = new TextField();
        txtPromo.setPromptText("Promo code");
        txtPromo.setStyle(
                "-fx-background-color: #17171C;" +
                "-fx-text-fill: white;" +
                "-fx-prompt-text-fill: #52525B;" +
                "-fx-border-color: #24242C;" +
                "-fx-border-radius: 6px;" +
                "-fx-background-radius: 6px;" +
                "-fx-padding: 7px 10px;" +
                "-fx-font-size: 11px;"
        );
        HBox.setHgrow(txtPromo, Priority.ALWAYS);

        Button btnApplyPromo = new Button("Apply");
        btnApplyPromo.setStyle("-fx-background-color: transparent; -fx-text-fill: #FF5722; -fx-font-size: 11px; -fx-font-weight: bold; -fx-cursor: hand;");
        btnApplyPromo.setOnAction(e -> {
            String code = txtPromo.getText().trim();
            if ("QUICKBITE".equalsIgnoreCase(code) || "EMBER".equalsIgnoreCase(code) || "FREE".equalsIgnoreCase(code)) {
                promoDiscount = 5.00;
                appliedPromo = code.toUpperCase();
                AlertUtil.showInfo("Promo Applied!", "Code '" + appliedPromo + "' applied! BDT 5.00 discount activated.");
                refreshCartDisplay();
            } else {
                AlertUtil.showWarning("Invalid Code", "Code '" + code + "' is not valid. Try 'QUICKBITE'.");
            }
        });

        promoBox.getChildren().addAll(txtPromo, btnApplyPromo);

        // Big Checkout Button
        btnPlaceOrder = new Button("Place order · BDT 0.00");
        btnPlaceOrder.setMaxWidth(Double.MAX_VALUE);
        btnPlaceOrder.setStyle(
                "-fx-background-color: #FF5722;" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 14px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 13px;" +
                "-fx-background-radius: 10px;" +
                "-fx-cursor: hand;"
        );
        btnPlaceOrder.setOnAction(e -> handleCheckout(stage));

        box.getChildren().addAll(header, lblCartSource, cartScroll, summary, promoBox, btnPlaceOrder);
        refreshCartDisplay();
        return box;
    }

    private Label styledLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #9CA3AF;");
        return lbl;
    }

    private void initDemoCartIfEmpty() {
        if (!cart.isEmpty()) return;

        // Populate cart with KFC Bangladesh items
        List<FoodItem> items = menuService.getFoodItems(selectedRestaurant != null ? selectedRestaurant.getId() : 1);
        FoodItem chicken = null;
        FoodItem burger = null;

        for (FoodItem fi : items) {
            if (fi.getName().toLowerCase().contains("crispy") || fi.getName().toLowerCase().contains("chicken")) chicken = fi;
            else if (fi.getName().toLowerCase().contains("zinger") || fi.getName().toLowerCase().contains("burger")) burger = fi;
        }

        if (chicken != null && burger != null) {
            cart.add(new CartItem(chicken, 2));
            cart.add(new CartItem(burger, 1));
        } else if (!items.isEmpty()) {
            cart.add(new CartItem(items.get(0), 2));
            if (items.size() > 1) cart.add(new CartItem(items.get(1), 1));
        }
    }

    private void refreshCartDisplay() {
        if (cartItemsBox == null) return;
        cartItemsBox.getChildren().clear();

        double subtotal = 0.0;
        int totalItems = 0;

        for (CartItem ci : cart) {
            subtotal += ci.getSubtotal();
            totalItems += ci.getQuantity();

            HBox itemRow = new HBox(8);
            itemRow.setAlignment(Pos.CENTER_LEFT);
            itemRow.setStyle("-fx-padding: 6px 0;");

            // Quantity stepper buttons
            HBox stepper = new HBox(4);
            stepper.setAlignment(Pos.CENTER);

            Button btnMinus = new Button("-");
            btnMinus.setStyle("-fx-background-color: #24242C; -fx-text-fill: #9CA3AF; -fx-font-size: 10px; -fx-padding: 1px 6px; -fx-background-radius: 4px; -fx-cursor: hand;");
            btnMinus.setOnAction(e -> {
                ci.decrementQuantity();
                refreshCartDisplay();
            });

            Label qtyLbl = new Label(String.valueOf(ci.getQuantity()));
            qtyLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: white; -fx-padding: 0 4px;");

            Button btnPlus = new Button("+");
            btnPlus.setStyle("-fx-background-color: #24242C; -fx-text-fill: #9CA3AF; -fx-font-size: 10px; -fx-padding: 1px 6px; -fx-background-radius: 4px; -fx-cursor: hand;");
            btnPlus.setOnAction(e -> {
                ci.incrementQuantity();
                refreshCartDisplay();
            });

            stepper.getChildren().addAll(btnMinus, qtyLbl, btnPlus);

            Label nameLbl = new Label(ci.getFoodItem().getName());
            nameLbl.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: white;");
            HBox.setHgrow(nameLbl, Priority.ALWAYS);

            Label priceLbl = new Label(String.format("BDT %.2f", ci.getSubtotal()));
            priceLbl.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: white;");

            Button btnDel = new Button("✕");
            btnDel.setStyle("-fx-background-color: transparent; -fx-text-fill: #EF4444; -fx-font-size: 10px; -fx-cursor: hand;");
            btnDel.setOnAction(e -> {
                cart.remove(ci);
                refreshCartDisplay();
            });

            itemRow.getChildren().addAll(stepper, nameLbl, priceLbl, btnDel);
            cartItemsBox.getChildren().add(itemRow);
        }

        double delivery = cart.isEmpty() ? 0.0 : 0.00; // Free delivery
        double service = cart.isEmpty() ? 0.0 : 2.50;
        double total = Math.max(0.0, subtotal + delivery + service - promoDiscount);

        if (lblCartCountBadge != null) lblCartCountBadge.setText(totalItems + " items");
        if (lblSubtotal != null) lblSubtotal.setText(String.format("BDT %.2f", subtotal));
        if (lblDeliveryFee != null) lblDeliveryFee.setText(delivery == 0.0 ? "Free" : String.format("BDT %.2f", delivery));
        if (lblServiceFee != null) lblServiceFee.setText(String.format("BDT %.2f", service));
        if (lblTotal != null) lblTotal.setText(String.format("BDT %.2f", total));
        if (btnPlaceOrder != null) btnPlaceOrder.setText(String.format("Place order · BDT %.2f", total));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. RESTAURANT MENU MODAL (Adds dishes with food images!)
    // ─────────────────────────────────────────────────────────────────────────

    private void showRestaurantMenuModal(Restaurant r) {
        Stage modal = new Stage();
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.setTitle(r.getName() + " — Full Menu");

        VBox root = new VBox(16);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: #111113;");

        // Header
        HBox head = new HBox(12);
        head.setAlignment(Pos.CENTER_LEFT);

        Label rTitle = new Label(r.getName());
        rTitle.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: white;");

        Label ratingLbl = new Label(String.format("★ %.1f", r.getRating()));
        ratingLbl.setStyle("-fx-background-color: #2D140D; -fx-text-fill: #FF5722; -fx-font-weight: bold; -fx-padding: 3px 8px; -fx-background-radius: 6px;");

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Button btnClose = new Button("✕ Close");
        btnClose.setStyle("-fx-background-color: #1F1F26; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 6px;");
        btnClose.setOnAction(e -> modal.close());

        head.getChildren().addAll(rTitle, ratingLbl, sp, btnClose);

        Label desc = new Label(r.getDescription());
        desc.setStyle("-fx-font-size: 12px; -fx-text-fill: #9CA3AF;");

        // Dishes Grid
        FlowPane dishesPane = new FlowPane();
        dishesPane.setHgap(14);
        dishesPane.setVgap(14);

        List<FoodItem> items = menuService.getFoodItems(r.getId());
        for (FoodItem item : items) {
            dishesPane.getChildren().add(buildFoodItemCard(item, modal));
        }

        ScrollPane scroll = new ScrollPane(dishesPane);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: #111113; -fx-border-width: 0;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        root.getChildren().addAll(head, desc, scroll);

        Scene s = new Scene(root, 760, 560);
        try {
            s.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        } catch (Exception ignored) {}
        modal.setScene(s);
        modal.show();
    }

    private VBox buildFoodItemCard(FoodItem item, Stage modal) {
        VBox card = new VBox(0);
        card.setPrefWidth(225);
        card.setStyle(
                "-fx-background-color: #17171C;" +
                "-fx-border-color: #25252E;" +
                "-fx-border-radius: 10px;" +
                "-fx-background-radius: 10px;"
        );

        // Food Image Banner
        ImageView imgView = new ImageView();
        imgView.setFitWidth(225);
        imgView.setFitHeight(115);
        imgView.setPreserveRatio(false);
        Rectangle clip = new Rectangle(225, 115);
        clip.setArcWidth(10);
        clip.setArcHeight(10);
        imgView.setClip(clip);

        boolean loaded = false;
        String path = item.getImageUrl();
        if (path != null && !path.isBlank() && !path.equals("food.png")) {
            try {
                File f = new File(path);
                String uri = f.exists() ? f.toURI().toString() : path;
                Image img = new Image(uri, 225, 115, false, true, true);
                imgView.setImage(img);
                loaded = true;
            } catch (Exception ignored) {}
        }

        if (!loaded) {
            StackPane placeholder = new StackPane();
            placeholder.setPrefSize(225, 115);
            placeholder.setStyle("-fx-background-color: #24242D; -fx-background-radius: 10px 10px 0 0;");
            Label icon = new Label(getRestaurantEmoji(item.getName()));
            icon.setStyle("-fx-font-size: 32px;");
            placeholder.getChildren().add(icon);
            card.getChildren().add(placeholder);
        } else {
            card.getChildren().add(imgView);
        }

        // Details
        VBox body = new VBox(6);
        body.setPadding(new Insets(10));

        Label name = new Label(item.getName());
        name.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: white;");
        name.setWrapText(true);

        Label desc = new Label(item.getDescription());
        desc.setStyle("-fx-font-size: 10px; -fx-text-fill: #9CA3AF;");
        desc.setWrapText(true);
        desc.setPrefHeight(32);

        HBox btm = new HBox();
        btm.setAlignment(Pos.CENTER_LEFT);
        Label price = new Label(String.format("BDT %.2f", item.getPrice()));
        price.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #FF5722;");

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Button btnAdd = new Button("+ Add");
        btnAdd.setStyle("-fx-background-color: #FF5722; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 6px; -fx-cursor: hand;");
        btnAdd.setOnAction(e -> {
            addToCart(item);
            AlertUtil.showInfo("Added to Cart", item.getName() + " was added to your cart!");
        });

        btm.getChildren().addAll(price, sp, btnAdd);
        body.getChildren().addAll(name, desc, btm);
        card.getChildren().add(body);
        return card;
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

    // ─────────────────────────────────────────────────────────────────────────
    // 5. CHECKOUT & LIVE ORDER TRACKING
    // ─────────────────────────────────────────────────────────────────────────

    private void handleCheckout(Stage stage) {
        if (cart.isEmpty()) {
            AlertUtil.showWarning("Empty Cart", "Please add items to your cart before proceeding.");
            return;
        }

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(stage);
        dialog.setTitle("QuickBite — Confirm Order");

        VBox form = new VBox(14);
        form.setPadding(new Insets(24));
        form.setStyle("-fx-background-color: #141417;");
        form.setPrefWidth(420);

        Label title = new Label("Review & Place Order");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");

        TextField txtAddress = new TextField(customer.getAddress() != null ? customer.getAddress() : "104 Sullivan St, SoHo");
        txtAddress.setStyle("-fx-background-color: #1D1D24; -fx-text-fill: white; -fx-border-color: #2F2F3B; -fx-border-radius: 6px; -fx-padding: 8px;");

        ComboBox<String> paymentCombo = new ComboBox<>();
        paymentCombo.getItems().addAll("Apple Pay", "Credit Card (•••• 4821)", "Cash on Delivery");
        paymentCombo.setValue("Apple Pay");
        paymentCombo.setMaxWidth(Double.MAX_VALUE);
        paymentCombo.setStyle("-fx-background-color: #1D1D24; -fx-border-color: #2F2F3B;");

        double subtotal = cart.stream().mapToDouble(CartItem::getSubtotal).sum();
        double total = Math.max(0.0, subtotal + 2.50 - promoDiscount);

        Label totalDue = new Label(String.format("Total Due: BDT %.2f (Free delivery + BDT 2.50 service fee)", total));
        totalDue.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #FF5722;");

        Button btnConfirm = new Button("Confirm & Pay · " + String.format("BDT %.2f", total));
        btnConfirm.setMaxWidth(Double.MAX_VALUE);
        btnConfirm.setStyle("-fx-background-color: #FF5722; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 12px; -fx-background-radius: 8px; -fx-cursor: hand;");

        btnConfirm.setOnAction(e -> {
            try {
                int restId = selectedRestaurant != null ? selectedRestaurant.getId() : 1;
                Order order = orderService.placeOrder(
                        customer.getId(),
                        restId,
                        cart,
                        txtAddress.getText(),
                        paymentCombo.getValue(),
                        0.00,
                        true
                );

                dialog.close();
                cart.clear();
                promoDiscount = 0.0;
                refreshCartDisplay();

                latestActiveOrder = order;
                AlertUtil.showInfo("Order Placed!", "Your order #" + order.getId() + " was placed successfully!");
                showLiveOrderTracking(stage, order.getId());

            } catch (Exception ex) {
                AlertUtil.showError("Order Error", ex.getMessage());
            }
        });

        form.getChildren().addAll(
                title,
                new Label("Delivery Address:") {{ setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 11px;"); }},
                txtAddress,
                new Label("Payment Method:") {{ setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 11px;"); }},
                paymentCombo,
                totalDue,
                new Region() {{ setPrefHeight(6); }},
                btnConfirm
        );

        Scene s = new Scene(form);
        try {
            s.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        } catch (Exception ignored) {}
        dialog.setScene(s);
        dialog.show();
    }

    private void showLiveOrderTracking(Stage stage, int orderId) {
        Stage trackStage = new Stage();
        trackStage.initModality(Modality.APPLICATION_MODAL);
        trackStage.setTitle("Live Tracking — Order #" + orderId);

        VBox root = new VBox(18);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: #141417;");

        Label title = new Label("Live Delivery Tracker: Order #" + orderId);
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");

        // 6 Lifecycle steps
        HBox stepsBox = new HBox(8);
        stepsBox.setAlignment(Pos.CENTER);
        stepsBox.setStyle("-fx-background-color: #1C1C23; -fx-padding: 14px; -fx-background-radius: 10px;");

        String[] stages = {"PLACED", "CONFIRMED", "PREPARING", "READY", "OUT_FOR_DELIVERY", "DELIVERED"};
        Map<String, Label> stageLabels = new LinkedHashMap<>();

        for (int i = 0; i < stages.length; i++) {
            Label step = new Label((i + 1) + ". " + stages[i].replace("_", " "));
            step.setStyle("-fx-padding: 6px 10px; -fx-background-radius: 6px; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-color: #272730; -fx-text-fill: #71717A;");
            stageLabels.put(stages[i], step);
            stepsBox.getChildren().add(step);
            if (i < stages.length - 1) {
                Label arrow = new Label("➔");
                arrow.setStyle("-fx-text-fill: #3F3F46;");
                stepsBox.getChildren().add(arrow);
            }
        }

        Label currentStatusBadge = new Label("STATUS: FETCHING...");
        currentStatusBadge.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #FF5722;");

        // Smart Delivery / Weather Advisory
        VBox weatherBox = new VBox(6);
        weatherBox.setStyle("-fx-background-color: #1A1F2C; -fx-border-color: #2A3B5C; -fx-border-radius: 8px; -fx-padding: 12px;");
        Label weatherTitle = new Label("🌦 Smart Delivery Intelligence (Live Weather API)");
        weatherTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #60A5FA; -fx-font-size: 12px;");

        Label weatherDetail = new Label("Fetching real-time weather advisory via background HttpClient...");
        weatherDetail.setStyle("-fx-font-size: 11px; -fx-text-fill: #93C5FD;");
        weatherDetail.setWrapText(true);
        weatherBox.getChildren().addAll(weatherTitle, weatherDetail);

        smartDeliveryService.fetchSmartDeliveryInfoAsync(info -> {
            Platform.runLater(() -> {
                weatherDetail.setText(String.format("Weather: %s (%.1f°C) | Estimated Delivery: ~%d mins\nAdvisory: %s",
                        info.getWeatherCondition(),
                        info.getTemperatureCelsius(),
                        info.getEstimatedMinutes(),
                        info.getWeatherAdvisory()));
            });
        });

        Runnable updateUI = () -> {
            Order o = orderService.getOrder(orderId);
            if (o != null) {
                currentStatusBadge.setText("CURRENT STATUS: " + o.getStatus().replace("_", " "));
                boolean passedCurrent = false;
                for (String s : stages) {
                    Label stepLbl = stageLabels.get(s);
                    if (s.equals(o.getStatus())) {
                        stepLbl.setStyle("-fx-padding: 6px 10px; -fx-background-radius: 6px; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-color: #FF5722; -fx-text-fill: white;");
                        passedCurrent = true;
                    } else if (!passedCurrent) {
                        stepLbl.setStyle("-fx-padding: 6px 10px; -fx-background-radius: 6px; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-color: #10B981; -fx-text-fill: white;");
                    } else {
                        stepLbl.setStyle("-fx-padding: 6px 10px; -fx-background-radius: 6px; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-color: #272730; -fx-text-fill: #71717A;");
                    }
                }
            }
        };

        com.quickbite.concurrency.OrderProcessingSimulator.getInstance().setStatusUpdateCallback((oid, newStatus) -> {
            if (oid == orderId) Platform.runLater(updateUI);
        });

        Button btnRefresh = new Button("🔄 Refresh Status");
        btnRefresh.setStyle("-fx-background-color: #272730; -fx-text-fill: white; -fx-cursor: hand;");
        btnRefresh.setOnAction(e -> updateUI.run());
        updateUI.run();

        root.getChildren().addAll(title, stepsBox, currentStatusBadge, weatherBox, btnRefresh);

        Scene s = new Scene(root, 720, 360);
        try {
            s.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        } catch (Exception ignored) {}
        trackStage.setScene(s);
        trackStage.show();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 6. ORDER HISTORY & PROFILE MODALS
    // ─────────────────────────────────────────────────────────────────────────

    private void showOrderHistoryModal(Stage ownerStage) {
        Stage histStage = new Stage();
        histStage.initModality(Modality.APPLICATION_MODAL);
        histStage.initOwner(ownerStage);
        histStage.setTitle("QuickBite — My Past Orders & Reviews");

        VBox root = new VBox(16);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: #111113;");

        Label title = new Label("Past Orders & Reviews");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");

        VBox ordersList = new VBox(12);
        ScrollPane scroll = new ScrollPane(ordersList);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: #111113; -fx-border-width: 0;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        List<Order> orders = orderService.getCustomerOrders(customer.getId());

        if (orders.isEmpty()) {
            Label noOrders = new Label("You have not placed any orders yet. Explore our top restaurants to get started!");
            noOrders.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 13px;");
            ordersList.getChildren().add(noOrders);
        } else {
            for (Order o : orders) {
                VBox card = new VBox(8);
                card.setStyle("-fx-background-color: #17171C; -fx-border-color: #24242C; -fx-border-radius: 10px; -fx-padding: 14px;");

                HBox h = new HBox(8);
                h.setAlignment(Pos.CENTER_LEFT);
                Label oId = new Label("Order #" + o.getId() + " • " + o.getRestaurantName());
                oId.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: white;");

                Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);

                Label statusBadge = new Label(o.getStatus());
                statusBadge.setStyle("-fx-background-color: #062818; -fx-text-fill: #22C55E; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 3px 8px; -fx-background-radius: 6px;");
                h.getChildren().addAll(oId, sp, statusBadge);

                Label dateLbl = new Label("Placed on: " + o.getCreatedAt() + " | Total: " + String.format("BDT %.2f", o.getTotalAmount()));
                dateLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #71717A;");

                HBox actions = new HBox(8);
                actions.setAlignment(Pos.CENTER_RIGHT);

                Button btnTrack = new Button("Track Order");
                btnTrack.setStyle("-fx-background-color: #FF5722; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold; -fx-cursor: hand;");
                btnTrack.setOnAction(e -> showLiveOrderTracking(ownerStage, o.getId()));

                Button btnReview = new Button("★ Rate & Review");
                btnReview.setStyle("-fx-background-color: #272730; -fx-text-fill: white; -fx-font-size: 11px; -fx-cursor: hand;");
                btnReview.setOnAction(e -> showReviewDialog(ownerStage, o));

                actions.getChildren().addAll(btnTrack, btnReview);
                card.getChildren().addAll(h, dateLbl, actions);
                ordersList.getChildren().add(card);
            }
        }

        root.getChildren().addAll(title, scroll);

        Scene s = new Scene(root, 640, 520);
        try {
            s.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        } catch (Exception ignored) {}
        histStage.setScene(s);
        histStage.show();
    }

    private void showReviewDialog(Stage ownerStage, Order order) {
        if (reviewDAO.hasUserReviewedOrder(customer.getId(), order.getId())) {
            AlertUtil.showInfo("Already Reviewed", "You have already reviewed Order #" + order.getId() + ".");
            return;
        }

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Rate & Review — Order #" + order.getId());

        VBox root = new VBox(12);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #141417;");

        Label title = new Label("Review: " + order.getRestaurantName());
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white;");

        ComboBox<Integer> ratingCombo = new ComboBox<>();
        ratingCombo.getItems().addAll(5, 4, 3, 2, 1);
        ratingCombo.setValue(5);
        ratingCombo.setStyle("-fx-background-color: #1E1E26;");

        TextArea commentArea = new TextArea();
        commentArea.setPromptText("How was the food taste, delivery speed, and packaging?");
        commentArea.setPrefRowCount(4);
        commentArea.setStyle("-fx-control-inner-background: #1E1E26; -fx-text-fill: white;");

        Button btnSubmit = new Button("Submit Review");
        btnSubmit.setMaxWidth(Double.MAX_VALUE);
        btnSubmit.setStyle("-fx-background-color: #FF5722; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10px; -fx-cursor: hand;");
        btnSubmit.setOnAction(e -> {
            Review r = new Review();
            r.setUserId(customer.getId());
            r.setRestaurantId(order.getRestaurantId());
            r.setOrderId(order.getId());
            r.setRating(ratingCombo.getValue());
            r.setComment(commentArea.getText());

            if (reviewDAO.create(r)) {
                AlertUtil.showInfo("Review Published", "Thank you! Your review for " + order.getRestaurantName() + " has been posted.");
                dialog.close();
            } else {
                AlertUtil.showError("Error", "Could not submit review.");
            }
        });

        root.getChildren().addAll(title, new Label("Rating:") {{ setStyle("-fx-text-fill: #9CA3AF;"); }}, ratingCombo, new Label("Feedback:") {{ setStyle("-fx-text-fill: #9CA3AF;"); }}, commentArea, btnSubmit);

        Scene s = new Scene(root, 400, 320);
        try {
            s.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        } catch (Exception ignored) {}
        dialog.setScene(s);
        dialog.show();
    }

    private void showReorderDialog(Stage stage, String restName, String items, String price) {
        AlertUtil.showInfo("Reorder Quick Action", "Re-ordering from " + restName + ":\n" + items + " (" + price + ")\nItems loaded into your current cart!");
    }

    private void showExploreDishesModal(Stage stage) {
        Stage modal = new Stage();
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.setTitle("Explore All Cuisines & Dishes");

        VBox root = new VBox(16);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: #111113;");

        Label title = new Label("Explore All Dishes Across Top Restaurants");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");

        FlowPane grid = new FlowPane();
        grid.setHgap(14);
        grid.setVgap(14);

        for (Restaurant r : restaurantDAO.getAll()) {
            for (FoodItem fi : menuService.getFoodItems(r.getId())) {
                grid.getChildren().add(buildFoodItemCard(fi, modal));
            }
        }

        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: #111113; -fx-border-width: 0;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        root.getChildren().addAll(title, scroll);

        Scene s = new Scene(root, 820, 600);
        try {
            s.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        } catch (Exception ignored) {}
        modal.setScene(s);
        modal.show();
    }

    private void showProfileModal(Stage stage) {
        Stage modal = new Stage();
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.setTitle("Customer Profile — QuickBite");

        VBox root = new VBox(14);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: #141417;");
        root.setPrefWidth(380);

        Label title = new Label("Customer Profile");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");

        Label nameLbl = new Label("Name: " + customer.getName());
        nameLbl.setStyle("-fx-text-fill: #E4E4E7; -fx-font-size: 13px;");

        Label emailLbl = new Label("Email: " + customer.getEmail());
        emailLbl.setStyle("-fx-text-fill: #E4E4E7; -fx-font-size: 13px;");

        Label phoneLbl = new Label("Phone: " + (customer.getPhone() != null ? customer.getPhone() : "+1 555-0101"));
        phoneLbl.setStyle("-fx-text-fill: #E4E4E7; -fx-font-size: 13px;");

        Label addrLbl = new Label("Delivery Address: " + (customer.getAddress() != null ? customer.getAddress() : "W 72nd St, New York"));
        addrLbl.setStyle("-fx-text-fill: #E4E4E7; -fx-font-size: 13px;");

        Label tierLbl = new Label("Membership: QuickBite Gold (Free Deliveries & 30% Off)");
        tierLbl.setStyle("-fx-text-fill: #FF5722; -fx-font-weight: bold; -fx-font-size: 12px;");

        Button btnClose = new Button("Close");
        btnClose.setMaxWidth(Double.MAX_VALUE);
        btnClose.setStyle("-fx-background-color: #272730; -fx-text-fill: white; -fx-padding: 8px; -fx-cursor: hand;");
        btnClose.setOnAction(e -> modal.close());

        root.getChildren().addAll(title, nameLbl, emailLbl, phoneLbl, addrLbl, tierLbl, btnClose);

        Scene s = new Scene(root);
        try {
            s.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        } catch (Exception ignored) {}
        modal.setScene(s);
        modal.show();
    }

    private void showAddressChangeDialog() {
        TextInputDialog tid = new TextInputDialog(customer.getAddress() != null ? customer.getAddress() : "W 72nd St, New York");
        tid.setTitle("Change Delivery Address");
        tid.setHeaderText(null);
        tid.setContentText("Enter your delivery address:");
        tid.showAndWait().ifPresent(newAddr -> {
            customer.setAddress(newAddr);
            AlertUtil.showInfo("Address Updated", "Delivery destination updated to: " + newAddr);
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 7. MANAGE PLACES (ADD & REMOVE RESTAURANTS)
    // ─────────────────────────────────────────────────────────────────────────

    private void showManageRestaurantsModal(Stage ownerStage) {
        Stage modal = new Stage();
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.initOwner(ownerStage);
        modal.setTitle("Manage Partner Restaurants — QuickBite");

        VBox root = new VBox(16);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: #111113;");

        // Header with Add Button
        HBox head = new HBox(12);
        head.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Partner Restaurants Directory");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Button btnAdd = new Button("➕ Add New Restaurant");
        btnAdd.setStyle("-fx-background-color: #FF5722; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 8px 14px; -fx-background-radius: 8px; -fx-cursor: hand;");
        btnAdd.setOnAction(e -> showAddRestaurantDialog(ownerStage, modal));

        Button btnClose = new Button("✕ Close");
        btnClose.setStyle("-fx-background-color: #1F1F26; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 6px;");
        btnClose.setOnAction(e -> modal.close());

        head.getChildren().addAll(title, sp, btnAdd, btnClose);

        Label sub = new Label("Add new partner places or remove restaurants from the customer marketplace.");
        sub.setStyle("-fx-font-size: 11px; -fx-text-fill: #9CA3AF;");

        // List of restaurants
        VBox list = new VBox(10);
        ScrollPane scroll = new ScrollPane(list);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: #111113; -fx-border-width: 0;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        Runnable populateList = () -> {
            list.getChildren().clear();
            List<Restaurant> all = restaurantDAO.getAll();
            for (Restaurant r : all) {
                HBox row = new HBox(12);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setStyle("-fx-background-color: #17171C; -fx-border-color: #24242C; -fx-border-radius: 10px; -fx-background-radius: 10px; -fx-padding: 12px 14px;");

                StackPane iconBox = new StackPane();
                iconBox.setPrefSize(40, 40);
                iconBox.setStyle("-fx-background-color: #24242C; -fx-background-radius: 8px;");
                Label iconLbl = new Label(getRestaurantEmoji(r.getName()));
                iconLbl.setStyle("-fx-font-size: 20px;");
                iconBox.getChildren().add(iconLbl);

                VBox details = new VBox(2);
                HBox.setHgrow(details, Priority.ALWAYS);

                HBox nameRow = new HBox(8);
                nameRow.setAlignment(Pos.CENTER_LEFT);
                Label nameLbl = new Label(r.getName());
                nameLbl.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: white;");

                Label ratingLbl = new Label(String.format("★ %.1f", r.getRating()));
                ratingLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #F59E0B;");
                nameRow.getChildren().addAll(nameLbl, ratingLbl);

                Label descLbl = new Label(r.getDescription() + "  •  📍 " + r.getAddress());
                descLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #9CA3AF;");

                details.getChildren().addAll(nameRow, descLbl);

                Button btnDelete = new Button("🗑 Remove");
                btnDelete.setStyle("-fx-background-color: #2D1414; -fx-text-fill: #EF4444; -fx-font-size: 11px; -fx-font-weight: bold; -fx-border-color: #4A1D1D; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-cursor: hand;");
                btnDelete.setOnAction(e -> {
                    if (AlertUtil.showConfirmation("Remove Restaurant", "Are you sure you want to delete '" + r.getName() + "' and its menu items?")) {
                        if (restaurantDAO.delete(r.getId())) {
                            AlertUtil.showInfo("Removed", "'" + r.getName() + "' was successfully removed.");
                            refreshRestaurants();
                            modal.close();
                            showManageRestaurantsModal(ownerStage);
                        } else {
                            AlertUtil.showError("Error", "Could not remove restaurant.");
                        }
                    }
                });

                row.getChildren().addAll(iconBox, details, btnDelete);
                list.getChildren().add(row);
            }
        };

        populateList.run();

        root.getChildren().addAll(head, sub, scroll);

        Scene s = new Scene(root, 720, 520);
        try {
            s.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        } catch (Exception ignored) {}
        modal.setScene(s);
        modal.show();
    }

    private void showAddRestaurantDialog(Stage ownerStage, Stage parentModal) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(ownerStage);
        dialog.setTitle("Add New Partner Restaurant");

        VBox form = new VBox(12);
        form.setPadding(new Insets(24));
        form.setStyle("-fx-background-color: #141417;");
        form.setPrefWidth(400);

        Label title = new Label("Add Restaurant Details");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");

        TextField txtName = new TextField();
        txtName.setPromptText("Restaurant Name (e.g. Bella Napoli)");
        styleDarkField(txtName);

        TextField txtDesc = new TextField();
        txtDesc.setPromptText("Cuisine / Description (e.g. Wood-fired Pizza)");
        styleDarkField(txtDesc);

        TextField txtAddr = new TextField();
        txtAddr.setPromptText("Address (e.g. 124 Broadway, SoHo)");
        styleDarkField(txtAddr);

        TextField txtPhone = new TextField();
        txtPhone.setPromptText("Phone (e.g. +1 555-9000)");
        styleDarkField(txtPhone);

        TextField txtRating = new TextField("4.8");
        txtRating.setPromptText("Initial Rating (1.0 to 5.0)");
        styleDarkField(txtRating);

        Button btnSave = new Button("Save & Publish Restaurant");
        btnSave.setMaxWidth(Double.MAX_VALUE);
        btnSave.setStyle("-fx-background-color: #FF5722; -fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 11px; -fx-background-radius: 8px; -fx-cursor: hand;");

        btnSave.setOnAction(e -> {
            String name = txtName.getText().trim();
            String desc = txtDesc.getText().trim();
            String addr = txtAddr.getText().trim();
            String phone = txtPhone.getText().trim();
            if (name.isEmpty() || desc.isEmpty()) {
                AlertUtil.showWarning("Missing Fields", "Please enter at least a Restaurant Name and Cuisine Description.");
                return;
            }

            double rating = 4.8;
            try {
                rating = Double.parseDouble(txtRating.getText().trim());
            } catch (Exception ignored) {}

            Restaurant r = new Restaurant(0, name, desc, addr, phone, rating, "kfc.png");
            if (restaurantDAO.create(r)) {
                AlertUtil.showInfo("Success!", "'" + name + "' is now live on the QuickBite marketplace!");
                dialog.close();
                if (parentModal != null) parentModal.close();
                refreshRestaurants();
                showManageRestaurantsModal(ownerStage);
            } else {
                AlertUtil.showError("Error", "Could not save restaurant.");
            }
        });

        form.getChildren().addAll(
                title,
                new Label("Restaurant Name:") {{ setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 11px;"); }},
                txtName,
                new Label("Cuisine / Tagline:") {{ setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 11px;"); }},
                txtDesc,
                new Label("Street Address:") {{ setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 11px;"); }},
                txtAddr,
                new Label("Contact Phone:") {{ setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 11px;"); }},
                txtPhone,
                new Label("Rating:") {{ setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 11px;"); }},
                txtRating,
                new Region() {{ setPrefHeight(6); }},
                btnSave
        );

        Scene s = new Scene(form);
        try {
            s.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        } catch (Exception ignored) {}
        dialog.setScene(s);
        dialog.show();
    }

    private void styleDarkField(TextField tf) {
        tf.setStyle(
                "-fx-background-color: #1D1D24;" +
                "-fx-text-fill: white;" +
                "-fx-prompt-text-fill: #52525B;" +
                "-fx-border-color: #2F2F3B;" +
                "-fx-border-radius: 6px;" +
                "-fx-background-radius: 6px;" +
                "-fx-padding: 8px 10px;" +
                "-fx-font-size: 12px;"
        );
        tf.setMaxWidth(Double.MAX_VALUE);
    }
}

