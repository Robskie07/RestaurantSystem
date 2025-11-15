package com.mycompany.restaurantsystem;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import javax.imageio.ImageIO;

interface MenuOperations {
    void resetOrder(); 
    double computeSubTotal();
    double computeTax();
    double computeTotal();
}

abstract class Product {
    private String name;
    private double price;
    private int quantity;

    public Product(String name, double price) {
        this.name = name;
        this.price = price;
        this.quantity = 0;
    }

    public final String getName() { return name; }
    public final double getPrice() { return price; }
    public final int getQuantity() { return quantity; }
    public final void setQuantity(int q) { this.quantity = q; }

    public abstract double getTotal();
}

class Food extends Product {
    public Food(String name, double price) { super(name, price); }
    @Override public double getTotal() { return getPrice() * getQuantity(); }
}

class Drink extends Product {
    public Drink(String name, double price) { super(name, price); }
    @Override public double getTotal() { return getPrice() * getQuantity(); }
}

class Staff {
    private String username;
    private String password;
    private String role;

    public Staff(String username, String password, String role) {
        this.username = username;
        this.password = password;
        this.role = role;
    }

    public String getUsername() { return username; }
    public String getRole() { return role; }

    public boolean checkPassword(String p) {
        return Objects.equals(password, p);
    }

    public static final java.util.List<Staff> STAFFS = new java.util.ArrayList<>();

    static {
        STAFFS.add(new Staff("Laurence", "admin123", "Manager"));
        STAFFS.add(new Staff("Joseph", "cashier001", "Cashier Counter 1"));
        STAFFS.add(new Staff("Mariel", "cashier002", "Cashier Counter 2"));
        STAFFS.add(new Staff("Alex", "cashier003", "Cashier Counter 3"));
        STAFFS.add(new Staff("Andrew", "chef001", "Head Chef"));
        STAFFS.add(new Staff("Mark", "waiter001", "Waiter"));
        STAFFS.add(new Staff("Anna", "waiter002", "Waiter"));
        STAFFS.add(new Staff("Jay", "waiter003", "Waiter"));
        STAFFS.add(new Staff("Mika", "waiter004", "Waiter"));
        STAFFS.add(new Staff("Rafael", "waiter005", "Waiter"));
        STAFFS.add(new Staff("Diana", "cook001", "Cook"));
        STAFFS.add(new Staff("Jonas", "cook002", "Cook"));
    }

    public static Staff authenticate(String username, String password) {
        for (Staff s : STAFFS) {
            if (s.getUsername().equals(username) && s.checkPassword(password)) {
                return s;
            }
        }
        return null;
    }
}

class Calc {
    public static final double TAX_RATE = 0.10;
    public static double add(double a, double b) { return a + b; }
    public static double add(double a, double b, double c) { return a + b + c; }
    public static String formatMoney(double v) { return String.format("%.2f", v); }
}

class TableOrder {
    int invoiceID;
    boolean occupied;
    String receiptText;
    int[] quantities;       
    boolean[] purchases;    
    
    TableOrder(int menuItemCount, int invoiceID) {
        this.invoiceID = invoiceID;
        this.occupied = false;
        this.receiptText = "";
        this.quantities = new int[menuItemCount];
        this.purchases = new boolean[menuItemCount];
        Arrays.fill(this.quantities, 0);
        Arrays.fill(this.purchases, false);
    }
}

public class RestaurantSystem extends JFrame implements MenuOperations {

    private static class MenuItemCard {
        Product product;
        String imagePath;
        JSpinner spinner;
        JCheckBox purchase;
        MenuItemCard(Product p, String img) { product = p; imagePath = img; }
    }

    private java.util.List<MenuItemCard> cards = new ArrayList<>();
    private JTextArea receiptArea = new JTextArea();
    private JTextField txtTax = new JTextField(8);
    private JTextField txtSubTotal = new JTextField(8);
    private JTextField txtTotal = new JTextField(8);
    private JLabel lblStaffInfo = new JLabel("Not logged in");
    private static int invoiceCounter = 1000;

    private JComboBox<String> tableCombo;
    private JComboBox<String> waiterCombo;

    // per-table orders
    private final TableOrder[] tableOrders;
    private final int TABLE_COUNT = 10;

    private JDialog receiptDialog = null;
    private JButton btnPrintReceipt;

    private final Color ORANGE = new Color(255, 165, 0);
    private final Color LIGHT_BROWN = new Color(210, 180, 140);
    private final Color COFFEE_BROWN = new Color(75, 46, 5);

    public RestaurantSystem(String title) {
        super(title);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(6, 6));
        setSize(1300, 700);
        setLocationRelativeTo(null);

        loadMenuItems();

        tableOrders = new TableOrder[TABLE_COUNT];
        for (int i = 0; i < TABLE_COUNT; i++) {
            tableOrders[i] = null;
        }

        JPanel top = new JPanel(new BorderLayout());
        top.setBorder(new EmptyBorder(8, 12, 8, 12));
        top.setBackground(ORANGE);
        top.setPreferredSize(new Dimension(0, 110));

        JPanel titlePanel = new JPanel(new GridLayout(2, 1));
        titlePanel.setOpaque(false);
        JLabel lblTitle = new JLabel("HARAYA'S DINING", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Serif", Font.BOLD, 30));
        lblTitle.setForeground(COFFEE_BROWN);
        JLabel lblTagline = new JLabel("\"Your Filipino Comfort Food Destination.\"", SwingConstants.CENTER);
        lblTagline.setFont(new Font("Serif", Font.ITALIC, 14));
        lblTagline.setForeground(COFFEE_BROWN);
        titlePanel.add(lblTitle);
        titlePanel.add(lblTagline);
        top.add(titlePanel, BorderLayout.CENTER);

        JPanel topRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        topRight.setOpaque(false);
        lblStaffInfo.setFont(new Font("SansSerif", Font.PLAIN, 12));

        JButton btnLogin = new JButton("Staff Login");
        JButton btnAddAccount = new JButton("Add Account");
        JButton btnListStaff = new JButton("List Staff");

        for (JButton b : new JButton[]{btnLogin, btnAddAccount, btnListStaff}) {
            b.setBackground(LIGHT_BROWN);
            b.setForeground(COFFEE_BROWN);
            b.setFont(new Font("SansSerif", Font.BOLD, 12));
            b.setFocusPainted(false);
            topRight.add(b);
        }

        topRight.add(new JLabel("Select Table:"));
        tableCombo = new JComboBox<>();
        updateTableCombo(); 
        tableCombo.setPreferredSize(new Dimension(180, 28));
        topRight.add(tableCombo);

        topRight.add(new JLabel("Waiter:"));
        waiterCombo = new JComboBox<>(new String[] {"mark", "anna", "jay", "mika", "rafael"});
        waiterCombo.setPreferredSize(new Dimension(120, 28));
        topRight.add(waiterCombo);

        topRight.add(lblStaffInfo);
        JLabel lblDate = new JLabel(getCurrentDateTime(), SwingConstants.RIGHT);
        lblDate.setForeground(COFFEE_BROWN);
        topRight.add(lblDate);

        top.add(topRight, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        btnLogin.addActionListener(e -> showStaffLoginDialog());
        btnAddAccount.addActionListener(e -> showAddAccountDialog());
        btnListStaff.addActionListener(e -> showStaffListDialog());

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setResizeWeight(0.72);

        JPanel grid = new JPanel();
        grid.setLayout(new BoxLayout(grid, BoxLayout.Y_AXIS));
        grid.setBorder(new EmptyBorder(10, 10, 10, 10));
        grid.setBackground(LIGHT_BROWN);

        grid.add(makeCategoryLabel("Main Dishes"));
        JPanel mainDishPanel = new JPanel(new GridLayout(0, 4, 10, 10));
        mainDishPanel.setBackground(LIGHT_BROWN);
        for (int i = 0; i < 10; i++) mainDishPanel.add(buildCardPanel(cards.get(i)));
        grid.add(mainDishPanel);

        grid.add(Box.createVerticalStrut(20));
        grid.add(makeCategoryLabel("Desserts"));
        JPanel dessertPanel = new JPanel(new GridLayout(0, 4, 10, 10));
        dessertPanel.setBackground(LIGHT_BROWN);
        for (int i = 10; i < 14; i++) dessertPanel.add(buildCardPanel(cards.get(i)));
        grid.add(dessertPanel);

        grid.add(Box.createVerticalStrut(20));
        grid.add(makeCategoryLabel("Drinks"));
        JPanel drinkPanel = new JPanel(new GridLayout(0, 4, 10, 10));
        drinkPanel.setBackground(LIGHT_BROWN);
        for (int i = 14; i < cards.size(); i++) drinkPanel.add(buildCardPanel(cards.get(i)));
        grid.add(drinkPanel);

        JScrollPane leftScroll = new JScrollPane(grid);
        split.setLeftComponent(leftScroll);

        JPanel right = new JPanel(new BorderLayout(6, 6));
        right.setBorder(new EmptyBorder(10, 10, 10, 10));
        right.setBackground(LIGHT_BROWN);
        receiptArea.setEditable(false);
        receiptArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        right.add(new JScrollPane(receiptArea), BorderLayout.CENTER);

        JPanel totals = new JPanel(new GridBagLayout());
        totals.setBackground(LIGHT_BROWN);
        totals.setBorder(new TitledBorder("Totals"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0; gbc.gridy = 0; totals.add(new JLabel("Tax:"), gbc);
        gbc.gridx = 1; txtTax.setEditable(false); totals.add(txtTax, gbc);
        gbc.gridx = 0; gbc.gridy = 1; totals.add(new JLabel("Sub Total:"), gbc);
        gbc.gridx = 1; txtSubTotal.setEditable(false); totals.add(txtSubTotal, gbc);
        gbc.gridx = 0; gbc.gridy = 2; totals.add(new JLabel("Total:"), gbc);
        gbc.gridx = 1; txtTotal.setEditable(false); totals.add(txtTotal, gbc);
        right.add(totals, BorderLayout.SOUTH);

        split.setRightComponent(right);
        add(split, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 10));
        bottom.setBackground(LIGHT_BROWN);

        JButton btnDone = new JButton("DONE");
        JButton btnTotal = new JButton("Total");
        JButton btnReceipt = new JButton("Receipt");
        btnPrintReceipt = new JButton("Print Receipt");
        JButton btnReset = new JButton("Reset");
        JButton btnExit = new JButton("Exit");

        for (JButton b : new JButton[]{btnDone, btnTotal, btnReceipt, btnPrintReceipt, btnReset, btnExit}) {
            b.setBackground(ORANGE);
            b.setForeground(COFFEE_BROWN);
            b.setFont(new Font("SansSerif", Font.BOLD, 14));
            b.setFocusPainted(false);
            bottom.add(b);
        }

        add(bottom, BorderLayout.SOUTH);

        btnDone.addActionListener(e -> doDone());
        btnTotal.addActionListener(e -> doTotal());
        btnReceipt.addActionListener(e -> doReceipt());
        btnPrintReceipt.addActionListener(e -> saveReceiptToFile());
        btnReset.addActionListener(e -> nextOrderAction());
        btnExit.addActionListener(e -> System.exit(0));

        tableCombo.addActionListener(e -> {
            int idx = tableCombo.getSelectedIndex();
            if (idx >= 0 && idx < TABLE_COUNT) {
                ensureTableOrderExists(idx);
                loadTableOrderToUI(idx);
            }
        });

        if (tableCombo.getItemCount() > 0) {
            tableCombo.setSelectedIndex(0);
            ensureTableOrderExists(0);
            loadTableOrderToUI(0);
        }

        setVisible(true);
    }

    private JLabel makeCategoryLabel(String title) {
       JLabel lbl = new JLabel(title, SwingConstants.CENTER);
       lbl.setFont(new Font("Times New Roman", Font.BOLD, 40));
       lbl.setForeground(COFFEE_BROWN); // keep text color
      lbl.setOpaque(false); // make background transparent
      lbl.setBorder(new LineBorder(LIGHT_BROWN, 5));
      lbl.setPreferredSize(new Dimension(50, 50));
      return lbl;
}


    private void loadMenuItems() {
        addMenuItem(new Food("Pancit", 150.0), "images/pancit.jpg");
        addMenuItem(new Food("Crispy Pata", 350.0), "images/crispy_pata.jpeg");
        addMenuItem(new Food("Beef Bulalo", 250.0), "images/beef_bulalo.jpg");
        addMenuItem(new Food("Chicken Adobo", 100.0), "images/chicken_adobo.jpeg");
        addMenuItem(new Food("Pork Sinigang", 140.0), "images/pork_sinigang.jpeg");
        addMenuItem(new Food("Beef Caldereta", 150.0), "images/beef_caldereta.jpeg");
        addMenuItem(new Food("Fried Bangus", 130.0), "images/fried_bangus.jpeg");
        addMenuItem(new Food("Sisig Rice Meal", 125.0), "images/sisig.jpeg");
        addMenuItem(new Food("Plain Rice", 135.0), "images/fried_rice.jpg");
        addMenuItem(new Food("Plain Rice", 25.0), "images/plain_rice.jpeg");

        addMenuItem(new Food("Strawberry Cake", 250.0), "images/strawberry_cake.jpg");
        addMenuItem(new Food("Chocolate Cake", 300.0), "images/chocolate_cake.jpg");
        addMenuItem(new Food("Fruits Cake", 500.0), "images/fruits_cake.jpg");
        addMenuItem(new Food("Rainbow Cake", 100.0), "images/rainbow_cake.jpg");

        addMenuItem(new Drink("Cold Coffee", 53.0), "images/cold_coffee.jpg");
        addMenuItem(new Drink("Cappuccino", 55.0), "images/cappuccino.jpg");
        addMenuItem(new Drink("Chocolate Coffee", 54.0), "images/chocolate_coffee.jpg");
        addMenuItem(new Drink("Green Tea", 52.0), "images/green_tea.jpg");
        addMenuItem(new Drink("Mineral Water", 6.5), "images/mineral_water.jpg");
        addMenuItem(new Drink("Coca Cola", 31.5), "images/coca_cola.jpg");
    }

    private void addMenuItem(Product p, String imagePath) {
        cards.add(new MenuItemCard(p, imagePath));
    }

    private JPanel buildCardPanel(MenuItemCard c) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBorder(new LineBorder(Color.GRAY, 1));
        card.setPreferredSize(new Dimension(180, 210));
        card.setBackground(Color.WHITE);

        JLabel imageLbl = new JLabel("", SwingConstants.CENTER);
        imageLbl.setPreferredSize(new Dimension(160, 130));
        ImageIcon icon = loadImageIcon(c.imagePath, 160, 100);
        if (icon != null) imageLbl.setIcon(icon);
        else imageLbl.setText("No Image");
        card.add(imageLbl, BorderLayout.NORTH);

        JPanel center = new JPanel(new GridLayout(0, 1));
        center.setBackground(Color.WHITE);
        JLabel name = new JLabel(c.product.getName(), SwingConstants.CENTER);
        name.setFont(new Font("Serif", Font.BOLD, 14));
        center.add(name);
        JLabel price = new JLabel("₱" + Calc.formatMoney(c.product.getPrice()), SwingConstants.CENTER);
        center.add(price);
        card.add(center, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottom.setBackground(Color.WHITE);
        SpinnerNumberModel sm = new SpinnerNumberModel(0, 0, 100, 1);
        JSpinner spinner = new JSpinner(sm);
        JCheckBox chk = new JCheckBox("Purchase");
        bottom.add(new JLabel("Quantity:"));
        bottom.add(spinner);
        bottom.add(chk);
        c.spinner = spinner;
        c.purchase = chk;
        card.add(bottom, BorderLayout.SOUTH);

        return card;
    }

    private ImageIcon loadImageIcon(String path, int w, int h) {
        try {
            File f = new File(path);
            if (!f.exists()) return null;
            Image img = ImageIO.read(f).getScaledInstance(w, h, Image.SCALE_SMOOTH);
            return new ImageIcon(img);
        } catch (IOException e) {
            return null;
        }
    }

    private void doTotal() {
        int idx = tableCombo.getSelectedIndex();
        if (idx < 0 || idx >= TABLE_COUNT) return;

        for (int i = 0; i < cards.size(); i++) {
            MenuItemCard c = cards.get(i);
            int q = (Integer) c.spinner.getValue();
            c.product.setQuantity(q);
        }
        txtSubTotal.setText(Calc.formatMoney(computeSubTotal()));
        txtTax.setText(Calc.formatMoney(computeTax()));
        txtTotal.setText(Calc.formatMoney(computeTotal()));
    }

    private void doReceipt() {
        int idx = tableCombo.getSelectedIndex();
        if (idx < 0 || idx >= TABLE_COUNT) {
            JOptionPane.showMessageDialog(this, "Please select a table.", "No Table Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        ensureTableOrderExists(idx);

        doTotal();

        TableOrder order = tableOrders[idx];
        StringBuilder sb = buildReceiptString(order.invoiceID, false);
        receiptArea.setText(sb.toString());
    }

    private StringBuilder buildReceiptString(int invoiceID, boolean includeTableInfo) {
        StringBuilder sb = new StringBuilder();
        sb.append("        HARAYA'S DINING\n");
        sb.append("  \"Your Filipino Comfort Food Destination.\"\n");
        sb.append("-------------------------------------------\n");
        sb.append("Invoice ID: ").append(invoiceID).append("\n");
        sb.append("Date: ").append(getCurrentDateTime()).append("\n");
        sb.append("-------------------------------------------\n");
        if (includeTableInfo) {
            int selectedTableIndex = tableCombo.getSelectedIndex();
            sb.append("Table: ").append("Table ").append(selectedTableIndex + 1).append("\n");
            sb.append("Waiter: ").append((String) waiterCombo.getSelectedItem()).append("\n");
            sb.append("-------------------------------------------\n");
        }
        sb.append(String.format("%-20s %5s %10s\n", "Item", "Qty", "Amount"));
        sb.append("-------------------------------------------\n");
        for (int i = 0; i < cards.size(); i++) {
            MenuItemCard c = cards.get(i);
            if (c.purchase.isSelected() && c.product.getQuantity() > 0) {
                sb.append(String.format("%-20s %5d %10s\n",
                        c.product.getName(),
                        c.product.getQuantity(),
                        Calc.formatMoney(c.product.getTotal())));
            }
        }
        sb.append("-------------------------------------------\n");
        sb.append(String.format("%-20s %15s\n", "Sub Total:", Calc.formatMoney(computeSubTotal())));
        sb.append(String.format("%-20s %15s\n", "Tax:", Calc.formatMoney(computeTax())));
        sb.append(String.format("%-20s %15s\n", "Total:", Calc.formatMoney(computeTotal())));
        sb.append("\nThank you! Come again.\n");
        return sb;
    }

    private void doDone() {
        int selectedTableIndex = tableCombo.getSelectedIndex();
        if (selectedTableIndex < 0 || selectedTableIndex >= TABLE_COUNT) {
            JOptionPane.showMessageDialog(this, "Please select a table.", "No Table Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        ensureTableOrderExists(selectedTableIndex);
        TableOrder order = tableOrders[selectedTableIndex];

        for (int i = 0; i < cards.size(); i++) {
            MenuItemCard c = cards.get(i);
            int q = (Integer) c.spinner.getValue();
            c.product.setQuantity(q);
        }

        doTotal();

        StringBuilder sb = buildReceiptString(order.invoiceID, true);
        String receiptText = sb.toString();

        order.receiptText = receiptText;
        order.occupied = true;
        for (int i = 0; i < cards.size(); i++) {
            order.quantities[i] = (Integer) cards.get(i).spinner.getValue();
            order.purchases[i] = cards.get(i).purchase.isSelected();
        }

        receiptArea.setText(receiptText);

        updateTableCombo(); 

        JTextArea popupArea = new JTextArea(receiptText);
        popupArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        popupArea.setEditable(false);
        JScrollPane sp = new JScrollPane(popupArea);
        sp.setPreferredSize(new Dimension(420, 360));

        JOptionPane pane = new JOptionPane(sp, JOptionPane.PLAIN_MESSAGE);
        JDialog dlg = pane.createDialog(this, "Receipt - Table " + (selectedTableIndex + 1));
        dlg.setModal(false);
        dlg.setResizable(true);
        dlg.setVisible(true);

        if (receiptDialog != null && receiptDialog.isShowing()) {
            receiptDialog.dispose();
        }
        receiptDialog = dlg;
    }

    @Override
    public void resetOrder() {
        int idx = tableCombo.getSelectedIndex();
        if (idx < 0 || idx >= TABLE_COUNT) return;

        ensureTableOrderExists(idx);
        TableOrder order = tableOrders[idx];

        for (int i = 0; i < cards.size(); i++) {
            cards.get(i).spinner.setValue(0);
            cards.get(i).purchase.setSelected(false);
            cards.get(i).product.setQuantity(0);
            order.quantities[i] = 0;
            order.purchases[i] = false;
        }
        order.receiptText = "";
        order.occupied = false;

        txtSubTotal.setText("");
        txtTax.setText("");
        txtTotal.setText("");
        receiptArea.setText("");

        updateTableCombo();
    }

    private void saveReceiptToFile() {
        String text = receiptArea.getText();
        if (text == null || text.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "No receipt to save. Generate a receipt first (DONE).", "Nothing to Save", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Receipt");
        chooser.setSelectedFile(new File("receipt.txt"));
        int res = chooser.showSaveDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            try (FileWriter fw = new FileWriter(f)) {
                fw.write(text);
                fw.flush();
                JOptionPane.showMessageDialog(this, "Receipt saved to: " + f.getAbsolutePath());
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error saving file: " + ex.getMessage(), "Save Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void nextOrderAction() {
        int selectedTableIndex = tableCombo.getSelectedIndex();
        if (selectedTableIndex < 0 || selectedTableIndex >= TABLE_COUNT) {
            JOptionPane.showMessageDialog(this, "Please select a table first.", "No Table Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        resetOrder();

        if (receiptDialog != null && receiptDialog.isShowing()) {
            receiptDialog.dispose();
            receiptDialog = null;
        }

        JOptionPane.showMessageDialog(this, "Ready for next order. Table " + (selectedTableIndex + 1) + " set to Available.");
    }

    @Override public double computeSubTotal() {
        return cards.stream().mapToDouble(c -> c.product.getTotal()).sum();
    }
    @Override public double computeTax() { return computeSubTotal() * Calc.TAX_RATE; }
    @Override public double computeTotal() { return computeSubTotal() + computeTax(); }

    private void showStaffLoginDialog() {
        JPanel panel = new JPanel(new GridLayout(0, 1));
        JTextField user = new JTextField();
        JPasswordField pass = new JPasswordField();
        panel.add(new JLabel("Username:")); panel.add(user);
        panel.add(new JLabel("Password:")); panel.add(pass);
        int ok = JOptionPane.showConfirmDialog(this, panel, "Staff Login", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (ok == JOptionPane.OK_OPTION) {
            Staff s = Staff.authenticate(user.getText().trim(), new String(pass.getPassword()));
            if (s != null) {
                lblStaffInfo.setText("Staff: " + s.getUsername() + " (" + s.getRole() + ")");
                JOptionPane.showMessageDialog(this, "Welcome, " + s.getUsername());
            } else {
                JOptionPane.showMessageDialog(this, "Login failed", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showAddAccountDialog() {
        JPanel panel = new JPanel(new GridLayout(0, 1));
        JTextField user = new JTextField();
        JPasswordField pass = new JPasswordField();
        JTextField role = new JTextField();
        panel.add(new JLabel("Username:")); panel.add(user);
        panel.add(new JLabel("Password:")); panel.add(pass);
        panel.add(new JLabel("Role:")); panel.add(role);

        int ok = JOptionPane.showConfirmDialog(this, panel, "Add New Staff Account", JOptionPane.OK_CANCEL_OPTION);
        if (ok == JOptionPane.OK_OPTION) {
            String u = user.getText().trim();
            String p = new String(pass.getPassword());
            String r = role.getText().trim();

            if (u.isEmpty() || p.isEmpty() || r.isEmpty()) {
                JOptionPane.showMessageDialog(this, "All fields are required!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            for (Staff s : Staff.STAFFS) {
                if (s.getUsername().equalsIgnoreCase(u)) {
                    JOptionPane.showMessageDialog(this, "Username already exists!", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }

            Staff.STAFFS.add(new Staff(u, p, r));
            JOptionPane.showMessageDialog(this, "New account added successfully!");
        }
    }

    private void showStaffListDialog() {
        StringBuilder sb = new StringBuilder("Registered Staff:\n");
        for (Staff s : Staff.STAFFS) {
            sb.append("- ").append(s.getUsername()).append(" (").append(s.getRole()).append(")\n");
        }
        JOptionPane.showMessageDialog(this, sb.toString(), "Staff List", JOptionPane.PLAIN_MESSAGE);
    }

    private void updateTableCombo() {
        int sel = (tableCombo.getSelectedIndex() >= 0) ? tableCombo.getSelectedIndex() : 0;
        tableCombo.removeAllItems();
        for (int i = 0; i < TABLE_COUNT; i++) {
            String label = "Table " + (i+1);
            if (tableOrders[i] != null && tableOrders[i].occupied) label += " - OCCUPIED";
            else label += " - Available";
            tableCombo.addItem(label);
        }
        if (sel >= 0 && sel < tableCombo.getItemCount()) tableCombo.setSelectedIndex(sel);
    }

    private void setTableOccupied(int index, boolean occupied) {
        ensureTableOrderExists(index);
        tableOrders[index].occupied = occupied;
        updateTableCombo();
        tableCombo.setSelectedIndex(index);
    }

    private void ensureTableOrderExists(int index) {
        if (tableOrders[index] == null) {
            invoiceCounter++;
            tableOrders[index] = new TableOrder(cards.size(), invoiceCounter);
        }
    }

    private void loadTableOrderToUI(int index) {
        ensureTableOrderExists(index);
        TableOrder order = tableOrders[index];

        for (int i = 0; i < cards.size(); i++) {
            cards.get(i).spinner.setValue(order.quantities[i]);
            cards.get(i).purchase.setSelected(order.purchases[i]);
            cards.get(i).product.setQuantity(order.quantities[i]);
        }

        if (order.receiptText != null && !order.receiptText.isEmpty()) {
            receiptArea.setText(order.receiptText);
            double sub = 0.0;
            for (int i = 0; i < cards.size(); i++) {
                sub += cards.get(i).product.getTotal();
            }
            txtSubTotal.setText(Calc.formatMoney(sub));
            txtTax.setText(Calc.formatMoney(sub * Calc.TAX_RATE));
            txtTotal.setText(Calc.formatMoney(sub + (sub * Calc.TAX_RATE)));
        } else {
            txtSubTotal.setText("");
            txtTax.setText("");
            txtTotal.setText("");
            receiptArea.setText(""); 
        }
    }

    public String getCurrentDateTime() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new RestaurantSystem("Haraya's Dining"));
    }
}
