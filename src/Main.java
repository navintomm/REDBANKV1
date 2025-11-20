import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.swing.*;

class Donor {
    private String name, phone, district, location, bloodGroup;

    public Donor(String name, String phone, String district, String location, String bloodGroup) {
        this.name = name;
        this.phone = phone;
        this.district = district;
        this.location = location;
        this.bloodGroup = bloodGroup.toUpperCase();
    }

    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    public String getDistrict() {
        return district;
    }

    public String getLocation() {
        return location;
    }

    public String getBloodGroup() {
        return bloodGroup;
    }

    public String getDetails() {
        return String.format("%-20s %-15s %-10s %-15s", name, phone, bloodGroup, location);
    }
}

class BloodGroupValidator {
    private static final Set<String> validBloodGroups = new HashSet<>(Arrays.asList(
            "A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-",
            "HH", "RH-NULL", "FY(A+)", "FY(A-)", "JK(A+)", "JK(A-)", "K+", "K-",
            "DI(A+)", "DI(A-)", "LU(A+)", "LU(A-)", "M+", "M-", "P1+", "P1-", "CO(A+)", "CO(A-)"
    ));

    public static boolean validate(String bloodGroup) {
        return validBloodGroups.contains(bloodGroup.toUpperCase());
    }
}

class DatabaseManager {
    private static final String URL = "jdbc:mysql://localhost:3306/redbank";
    private static final String USER = "root";
    private static final String PASSWORD = "6282";

    public Connection connect() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public void initializeDatabase() {
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/", "root", "6282")) {
            Statement stmt = conn.createStatement();
            stmt.execute("CREATE DATABASE IF NOT EXISTS redbank");
            stmt.close();
            
            try (Connection dbConn = connect();
                 Statement dbStmt = dbConn.createStatement()) {
                String createTableSQL = "CREATE TABLE IF NOT EXISTS donors (" +
                                       "id INT AUTO_INCREMENT PRIMARY KEY," +
                                       "name VARCHAR(100) NOT NULL," +
                                       "phone VARCHAR(20) NOT NULL," +
                                       "district VARCHAR(50) NOT NULL," +
                                       "location VARCHAR(100) NOT NULL," +
                                       "blood_group VARCHAR(10) NOT NULL)";
                dbStmt.execute(createTableSQL);
                System.out.println("✅ Database initialized successfully.");
            }
        } catch (SQLException e) {
            System.out.println("❌ Database initialization error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public boolean isDuplicate(String name, String phone, String bloodGroup, String district) {
        String query = "SELECT COUNT(*) FROM donors WHERE name = ? AND phone = ? AND blood_group = ? AND district = ?";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, name);
            pstmt.setString(2, phone);
            pstmt.setString(3, bloodGroup.toUpperCase());
            pstmt.setString(4, district);
            
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.out.println("❌ Error checking for duplicate donor: " + e.getMessage());
        }
        return false;
    }

    public void addDonor(Donor donor) {
        String sql = "INSERT INTO donors (name, phone, district, location, blood_group) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, donor.getName());
            stmt.setString(2, donor.getPhone());
            stmt.setString(3, donor.getDistrict());
            stmt.setString(4, donor.getLocation());
            stmt.setString(5, donor.getBloodGroup());
            
            stmt.executeUpdate();
            System.out.println(" Donor Registered Successfully!");
        } catch (SQLException e) {
            System.out.println("❌ Error adding donor: " + e.getMessage());
        }
    }

    public ArrayList<Donor> searchByDistrictAndBloodGroup(String district, String bloodGroup) {
        ArrayList<Donor> result = new ArrayList<>();
        String sql = "SELECT name, phone, district, location, blood_group FROM donors WHERE district = ? AND blood_group = ?";
        
        try (Connection conn = connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, district);
            stmt.setString(2, bloodGroup.toUpperCase());
            
            ResultSet rs = stmt.executeQuery();
            
            if (!rs.isBeforeFirst()) {
                return result;
            }
            
            while (rs.next()) {
                Donor donor = new Donor(
                    rs.getString("name"),
                    rs.getString("phone"),
                    rs.getString("district"),
                    rs.getString("location"),
                    rs.getString("blood_group")
                );
                result.add(donor);
            }
        } catch (SQLException e) {
            System.out.println("❌ Error searching for donors: " + e.getMessage());
        }
        
        return result;
    }
}

class GradientPanel extends JPanel {
    private Color color1;
    private Color color2;
    
    public GradientPanel(Color color1, Color color2) {
        this.color1 = color1;
        this.color2 = color2;
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        int width = getWidth();
        int height = getHeight();
        GradientPaint gp = new GradientPaint(0, 0, color1, 0, height, color2);
        g2d.setPaint(gp);
        g2d.fillRect(0, 0, width, height);
    }
}

public class Main {
    private static DatabaseManager dbManager = new DatabaseManager();
    private static JFrame frame;
    private static JTextArea displayArea;
    private static JTextField nameField, phoneField, districtField, locationField, bloodGroupField;
    private static Font titleFont = new Font("Arial", Font.BOLD, 18);
    private static Font labelFont = new Font("Arial", Font.BOLD, 14);
    private static Font buttonFont = new Font("Arial", Font.BOLD, 14);
    private static Color primaryColor = new Color(220, 53, 69); // Red color
    private static Color secondaryColor = new Color(248, 249, 250); // Light background
    private static Color accentColor = new Color(25, 135, 84); // Green for success
    
    public static void main(String[] args) {
        try {
            // Set system look and feel
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        dbManager.initializeDatabase();
        createGUI();
    }
    
    private static void createGUI() {
        frame = new JFrame("Red Bank Blood Donation");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setLayout(new BorderLayout());
        
        // Create header panel with logo and title
        JPanel headerPanel = new GradientPanel(primaryColor, new Color(240, 80, 80));
        headerPanel.setLayout(new BorderLayout());
        headerPanel.setPreferredSize(new Dimension(800, 100));
        
        JLabel titleLabel = new JLabel("Red Bank Blood Donation System", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        titleLabel.setForeground(Color.WHITE);
        headerPanel.add(titleLabel, BorderLayout.CENTER);
        
        // Create icon for the blood drop
        JLabel iconLabel = new JLabel("🩸", JLabel.CENTER);
        iconLabel.setFont(new Font("Arial", Font.PLAIN, 48));
        iconLabel.setForeground(Color.WHITE);
        headerPanel.add(iconLabel, BorderLayout.WEST);
        
        // Add subtitle
        JLabel subtitleLabel = new JLabel("Save Lives - Donate Blood", JLabel.CENTER);
        subtitleLabel.setFont(new Font("Arial", Font.ITALIC, 16));
        subtitleLabel.setForeground(new Color(255, 255, 255, 200));
        headerPanel.add(subtitleLabel, BorderLayout.SOUTH);
        
        frame.add(headerPanel, BorderLayout.NORTH);
        
        // Main content panel with gradient background
        JPanel mainPanel = new GradientPanel(new Color(250, 250, 250), new Color(245, 245, 245));
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        
        // Create a center panel for buttons and welcome message
        JPanel centerPanel = new JPanel(new BorderLayout(0, 20));
        centerPanel.setOpaque(false);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(20, 50, 20, 50));
        
        // Welcome message
        JTextPane welcomeText = new JTextPane();
        welcomeText.setContentType("text/html");
        welcomeText.setText("<html><div style='text-align: center; font-family: Arial;'><h2 style='color: #dc3545;'>Welcome to Red Bank</h2>" +
                           "<p style='font-size: 14px; color: #333;'>A platform connecting blood donors with those in need. " +
                           "Register as a donor or search for available donors in your area.</p></div></html>");
        welcomeText.setEditable(false);
        welcomeText.setBackground(new Color(0, 0, 0, 0));
        welcomeText.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        centerPanel.add(welcomeText, BorderLayout.NORTH);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 50, 0, 50));

        JButton registerButton = createStyledButton("Register Donor", "person-add.png", accentColor);
        registerButton.addActionListener(e -> showRegistrationForm());
        
        JButton searchButton = createStyledButton("Search Donors", "search.png", primaryColor);
        searchButton.addActionListener(e -> showSearchForm());
        
        buttonPanel.add(registerButton);
        buttonPanel.add(searchButton);
        
        centerPanel.add(buttonPanel, BorderLayout.CENTER);
        
        // Add some statistics (placeholder)
        JPanel statsPanel = new JPanel(new GridLayout(1, 3, 10, 0));
        statsPanel.setOpaque(false);
        statsPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));
        
        statsPanel.add(createStatPanel("1000+", "Donors Registered"));
        statsPanel.add(createStatPanel("50+", "Districts Covered"));
        statsPanel.add(createStatPanel("24/7", "Availability"));
        
        centerPanel.add(statsPanel, BorderLayout.SOUTH);
        
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        
        // Display area with styled border (smaller now)
        displayArea = new JTextArea();
        displayArea.setEditable(false);
        displayArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        displayArea.setBackground(new Color(252, 252, 252));
        displayArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JScrollPane scrollPane = new JScrollPane(displayArea);
        scrollPane.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(5, 0, 0, 0),
            BorderFactory.createLineBorder(new Color(220, 220, 220), 1)
        ));
        scrollPane.setPreferredSize(new Dimension(800, 80));
        
        mainPanel.add(scrollPane, BorderLayout.SOUTH);
        
        frame.add(mainPanel, BorderLayout.CENTER);
        
        // Footer panel
        JPanel footerPanel = new JPanel();
        footerPanel.setBackground(new Color(52, 58, 64));
        footerPanel.setPreferredSize(new Dimension(800, 30));
        JLabel footerLabel = new JLabel("© 2025 Red Bank Blood Donation System | Contact: help@redbank.org", JLabel.CENTER);
        footerLabel.setForeground(Color.WHITE);
        footerPanel.add(footerLabel);
        
        frame.add(footerPanel, BorderLayout.SOUTH);
        
        // Center frame on screen
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
    
    private static JPanel createStatPanel(String value, String label) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        
        JLabel valueLabel = new JLabel(value, JLabel.CENTER);
        valueLabel.setFont(new Font("Arial", Font.BOLD, 24));
        valueLabel.setForeground(primaryColor);
        
        JLabel descLabel = new JLabel(label, JLabel.CENTER);
        descLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        descLabel.setForeground(new Color(100, 100, 100));
        
        panel.add(valueLabel, BorderLayout.CENTER);
        panel.add(descLabel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private static JButton createStyledButton(String text, String iconName, Color color) {
        JButton button = new JButton(text);
        button.setFont(buttonFont);
        button.setForeground(Color.WHITE);
        button.setBackground(color);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(true);
        button.setOpaque(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(200, 50));
        button.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        return button;
    }

    private static JTextField createStyledTextField() {
        JTextField field = new JTextField();
        field.setPreferredSize(new Dimension(200, 30));
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        return field;
    }

    private static void showRegistrationForm() {
        frame.getContentPane().removeAll();
        
        // Header
        JPanel headerPanel = new GradientPanel(primaryColor, new Color(240, 80, 80));
        headerPanel.setLayout(new BorderLayout());
        headerPanel.setPreferredSize(new Dimension(800, 60));
        
        JLabel titleLabel = new JLabel("Register New Donor", JLabel.CENTER);
        titleLabel.setFont(titleFont);
        titleLabel.setForeground(Color.WHITE);
        headerPanel.add(titleLabel, BorderLayout.CENTER);
        
        frame.add(headerPanel, BorderLayout.NORTH);
        
        // Form panel
        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BorderLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(20, 50, 20, 50));
        formPanel.setBackground(secondaryColor);
        
        JPanel fieldsPanel = new JPanel(new GridLayout(5, 2, 10, 15));
        fieldsPanel.setBackground(secondaryColor);
        fieldsPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
            BorderFactory.createEmptyBorder(25, 25, 25, 25)
        ));
        
        // Name field
        JLabel nameLabel = new JLabel("Name:");
        nameLabel.setFont(labelFont);
        fieldsPanel.add(nameLabel);
        
        nameField = createStyledTextField();
        fieldsPanel.add(nameField);
        
        // Phone field
        JLabel phoneLabel = new JLabel("Phone:");
        phoneLabel.setFont(labelFont);
        fieldsPanel.add(phoneLabel);
        
        phoneField = createStyledTextField();
        fieldsPanel.add(phoneField);
        
        // District field
        JLabel districtLabel = new JLabel("District:");
        districtLabel.setFont(labelFont);
        fieldsPanel.add(districtLabel);
        
        districtField = createStyledTextField();
        fieldsPanel.add(districtField);
        
        // Location field
        JLabel locationLabel = new JLabel("Location:");
        locationLabel.setFont(labelFont);
        fieldsPanel.add(locationLabel);
        
        locationField = createStyledTextField();
        fieldsPanel.add(locationField);
        
        // Blood Group field
        JLabel bloodGroupLabel = new JLabel("Blood Group:");
        bloodGroupLabel.setFont(labelFont);
        fieldsPanel.add(bloodGroupLabel);
        
        bloodGroupField = createStyledTextField();
        fieldsPanel.add(bloodGroupField);
        
        // Add ActionListeners for Enter key navigation
        nameField.addActionListener(new NextFieldActionListener(phoneField));
        phoneField.addActionListener(new NextFieldActionListener(districtField));
        districtField.addActionListener(new NextFieldActionListener(locationField));
        locationField.addActionListener(new NextFieldActionListener(bloodGroupField));
        bloodGroupField.addActionListener(e -> registerDonor());
        
        formPanel.add(fieldsPanel, BorderLayout.CENTER);
        
        // Buttons panel
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 20));
        buttonsPanel.setBackground(secondaryColor);
        
        JButton registerDonorButton = createStyledButton("Register", "save.png", accentColor);
        registerDonorButton.addActionListener(e -> registerDonor());
        buttonsPanel.add(registerDonorButton);
        
        JButton backButton = createStyledButton("Back", "back.png", new Color(108, 117, 125));
        backButton.addActionListener(e -> createGUI());
        buttonsPanel.add(backButton);
        
        formPanel.add(buttonsPanel, BorderLayout.SOUTH);
        
        frame.add(formPanel, BorderLayout.CENTER);
        
        // Result display area
        displayArea = new JTextArea();
        displayArea.setEditable(false);
        displayArea.setFont(new Font("Arial", Font.PLAIN, 14));
        displayArea.setBackground(new Color(252, 252, 252));
        displayArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JScrollPane scrollPane = new JScrollPane(displayArea);
        scrollPane.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(0, 50, 20, 50),
            BorderFactory.createLineBorder(new Color(220, 220, 220), 1)
        ));
        scrollPane.setPreferredSize(new Dimension(800, 100));
        
        frame.add(scrollPane, BorderLayout.SOUTH);
        
        // Focus on first field
        nameField.requestFocusInWindow();
        
        frame.revalidate();
        frame.repaint();
    }

    private static void showSearchForm() {
        frame.getContentPane().removeAll();
        
        // Header
        JPanel headerPanel = new GradientPanel(primaryColor, new Color(240, 80, 80));
        headerPanel.setLayout(new BorderLayout());
        headerPanel.setPreferredSize(new Dimension(800, 60));
        
        JLabel titleLabel = new JLabel("Search Donors", JLabel.CENTER);
        titleLabel.setFont(titleFont);
        titleLabel.setForeground(Color.WHITE);
        headerPanel.add(titleLabel, BorderLayout.CENTER);
        
        frame.add(headerPanel, BorderLayout.NORTH);
        
        // Search panel
        JPanel searchPanel = new JPanel();
        searchPanel.setLayout(new BorderLayout());
        searchPanel.setBorder(BorderFactory.createEmptyBorder(20, 50, 20, 50));
        searchPanel.setBackground(secondaryColor);
        
        JPanel fieldsPanel = new JPanel(new GridLayout(2, 2, 10, 15));
        fieldsPanel.setBackground(secondaryColor);
        fieldsPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
            BorderFactory.createEmptyBorder(25, 25, 25, 25)
        ));
        
        // Blood Group field
        JLabel bloodGroupLabel = new JLabel("Blood Group:");
        bloodGroupLabel.setFont(labelFont);
        fieldsPanel.add(bloodGroupLabel);
        
        bloodGroupField = createStyledTextField();
        fieldsPanel.add(bloodGroupField);
        
        // District field
        JLabel districtLabel = new JLabel("District:");
        districtLabel.setFont(labelFont);
        fieldsPanel.add(districtLabel);
        
        districtField = createStyledTextField();
        fieldsPanel.add(districtField);
        
        // Add ActionListeners for Enter key navigation
        bloodGroupField.addActionListener(new NextFieldActionListener(districtField));
        districtField.addActionListener(e -> searchDonors());
        
        searchPanel.add(fieldsPanel, BorderLayout.CENTER);
        
        // Buttons panel
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 20));
        buttonsPanel.setBackground(secondaryColor);
        
        JButton searchButton = createStyledButton("Search", "search.png", primaryColor);
        searchButton.addActionListener(e -> searchDonors());
        buttonsPanel.add(searchButton);
        
        JButton backButton = createStyledButton("Back", "back.png", new Color(108, 117, 125));
        backButton.addActionListener(e -> createGUI());
        buttonsPanel.add(backButton);
        
        searchPanel.add(buttonsPanel, BorderLayout.SOUTH);
        
        frame.add(searchPanel, BorderLayout.NORTH);
        
        // Results panel
        JPanel resultsPanel = new JPanel(new BorderLayout());
        resultsPanel.setBorder(BorderFactory.createEmptyBorder(0, 50, 20, 50));
        resultsPanel.setBackground(secondaryColor);
        
        displayArea = new JTextArea();
        displayArea.setEditable(false);
        displayArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        displayArea.setBackground(new Color(252, 252, 252));
        displayArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JScrollPane scrollPane = new JScrollPane(displayArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220), 1));
        
        resultsPanel.add(scrollPane, BorderLayout.CENTER);
        
        frame.add(resultsPanel, BorderLayout.CENTER);
        
        // Focus on first field
        bloodGroupField.requestFocusInWindow();
        
        frame.revalidate();
        frame.repaint();
    }

    private static void registerDonor() {
        String name = nameField.getText();
        String phone = phoneField.getText();
        String district = districtField.getText();
        String location = locationField.getText();
        String bloodGroup = bloodGroupField.getText().toUpperCase();
        
        if (name.isEmpty() || phone.isEmpty() || district.isEmpty() || location.isEmpty() || bloodGroup.isEmpty()) {
            showMessage("❌ All fields are required! Please fill all information.", Color.RED);
            return;
        }
        
        if (!BloodGroupValidator.validate(bloodGroup)) {
            showMessage("❌ Invalid Blood Group! Try again.", Color.RED);
            return;
        }
        
        if (dbManager.isDuplicate(name, phone, bloodGroup, district)) {
            showMessage("❌ Donor already exists in the database!", Color.RED);
            return;
        }
        
        Donor newDonor = new Donor(name, phone, district, location, bloodGroup);
        dbManager.addDonor(newDonor);
        showMessage("✅ Donor Registered Successfully!", accentColor);
    }
    
    private static void searchDonors() {
        String district = districtField.getText();
        String bloodGroup = bloodGroupField.getText().toUpperCase();
        
        if (district.isEmpty() || bloodGroup.isEmpty()) {
            showMessage("❌ Please enter both district and blood group!", Color.RED);
            return;
        }
        
        ArrayList<Donor> foundDonors = dbManager.searchByDistrictAndBloodGroup(district, bloodGroup);
        
        if (foundDonors.isEmpty()) {
            showMessage("❌ No donors with blood group " + bloodGroup + " found in " + district, Color.RED);
        } else {
            displaySearchResults(foundDonors, district, bloodGroup);
        }
    }
    
    private static void showMessage(String message, Color color) {
        displayArea.setForeground(color);
        displayArea.setText(message);
    }
    
    private static void displaySearchResults(ArrayList<Donor> donors, String district, String bloodGroup) {
        // Create a styled HTML table for results
        StringBuilder html = new StringBuilder();
        html.append("<html><body style='font-family: Arial; width: 100%'>");
        html.append("<div style='background-color: #dc3545; color: white; padding: 10px; text-align: center; font-size: 16px; font-weight: bold;'>");
        html.append("🔎 Found ").append(donors.size()).append(" donors with ").append(bloodGroup).append(" in ").append(district);
        html.append("</div>");
        html.append("<table style='width: 100%; border-collapse: collapse;'>");
        html.append("<tr style='background-color: #f8f9fa;'>");
        html.append("<th style='padding: 10px; text-align: left; border-bottom: 2px solid #dee2e6;'>Name</th>");
        html.append("<th style='padding: 10px; text-align: left; border-bottom: 2px solid #dee2e6;'>Phone</th>");
        html.append("<th style='padding: 10px; text-align: left; border-bottom: 2px solid #dee2e6;'>Blood Group</th>");
        html.append("<th style='padding: 10px; text-align: left; border-bottom: 2px solid #dee2e6;'>Location</th>");
        html.append("</tr>");
        
        for (int i = 0; i < donors.size(); i++) {
            Donor donor = donors.get(i);
            String rowColor = (i % 2 == 0) ? "#ffffff" : "#f2f2f2";
            
            html.append("<tr style='background-color: ").append(rowColor).append(";'>");
            html.append("<td style='padding: 8px; border-bottom: 1px solid #dee2e6;'>").append(donor.getName()).append("</td>");
            html.append("<td style='padding: 8px; border-bottom: 1px solid #dee2e6;'>").append(donor.getPhone()).append("</td>");
            
            // Add some color to the blood group cell based on type
            String bloodGroupColor = getBloodGroupColor(donor.getBloodGroup());
            html.append("<td style='padding: 8px; border-bottom: 1px solid #dee2e6; color: ").append(bloodGroupColor).append(";'>");
            html.append("<strong>").append(donor.getBloodGroup()).append("</strong></td>");
            
            html.append("<td style='padding: 8px; border-bottom: 1px solid #dee2e6;'>").append(donor.getLocation()).append("</td>");
            html.append("</tr>");
        }
        
        html.append("</table>");
        html.append("</body></html>");
        
        JEditorPane editorPane = new JEditorPane("text/html", html.toString());
        editorPane.setEditable(false);
        editorPane.setBackground(new Color(252, 252, 252));
        
        JScrollPane scrollPane = new JScrollPane(editorPane);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220), 1));
        
        frame.getContentPane().remove(frame.getContentPane().getComponentCount() - 1);
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.revalidate();
        frame.repaint();
    }
    
    private static String getBloodGroupColor(String bloodGroup) {
        switch (bloodGroup) {
            case "A+":
            case "A-":
                return "#dc3545"; // Red
            case "B+":
            case "B-":
                return "#0d6efd"; // Blue
            case "AB+":
            case "AB-":
                return "#6f42c1"; // Purple
            case "O+":
            case "O-":
                return "#198754"; // Green
            default:
                return "#000000"; // Black for other rare types
        }
    }
    
    // Custom ActionListener for moving to next field on Enter key press
    private static class NextFieldActionListener implements ActionListener {
        private JTextField nextField;
        
        public NextFieldActionListener(JTextField nextField) {
            this.nextField = nextField;
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            nextField.requestFocusInWindow();
        }
    }
}