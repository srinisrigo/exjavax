import java.awt.*;
import javax.swing.*;
import java.util.*;
import java.util.List;
import java.sql.*;
import javax.swing.table.*;
import java.lang.*;
import java.awt.event.*;
import javax.swing.border.*;

enum EPageMode {
    SIGNIN, RECORDS, FORM, LOGOUT
};

enum EQuery {
    PATIENTS(0),
    STUDIES(1),
    DICOMS(2),
    AUTHENTICATE(3);

    private int value;
    private static Map map = new HashMap<>();

    private EQuery(int value) {
        this.value = value;
    }

    static {
        for (EQuery eQuery : EQuery.values()) {
            map.put(eQuery.value, eQuery);
        }
    }

    public static EQuery valueOf(int eQuery) {
        return (EQuery) map.get(eQuery);
    }

    public int getValue() {
        return value;
    }
};

public class Index implements WindowListener, ActionListener {
    private Connection connection;
    List<JFrame> jfList = new ArrayList<JFrame>();
    private JTextField jtfUsername, jtfPassword;
    private JLabel jlError;
    private Statement statement;

    public static void main(String[] args) {
        Index exobj = new Index();
        exobj.checkDB();
        exobj.setFrames();
    }

    private void checkDB() {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println(e);
            System.exit(-1);
        }
        try {
            connection = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:5432/APR05", "postgres",
                    "1q2w3e4r5t");
            statement = connection.createStatement();
        } catch (java.sql.SQLException e) {
            System.err.println(e);
            System.exit(-1);
        }
    }

    private void setFrames() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] gds = ge.getScreenDevices();
        for (int d = 0; d < gds.length; d++) {
            GraphicsDevice gd = gds[d];
            GraphicsConfiguration[] gc = gd.getConfigurations();
            for (int c = 0; c < gc.length; c++) {
                JFrame jf = new JFrame(gd.getDefaultConfiguration());
                jf.setTitle("Authenticate");
                jf.setExtendedState(JFrame.MAXIMIZED_BOTH);
                jf.addWindowListener(this);
                Rectangle rect = gc[c].getBounds();
                jf.setLocation(rect.x, rect.y);
                if (d == 0) this.setSign(jf);
                jf.setVisible(true);
                jfList.add(jf);
            }
        }
    }

    private String getQuery(String query) {
        String querystr;
        switch (EQuery.valueOf(query)) {
        case PATIENTS:
            querystr = "SELECT p.account_no, p.full_name, p.id as patient_id, p.phonogram_name, p.ideogram_name, p.birth_date::text, p.gender, '' as last_study_date, p.patient_info, nullif(p.patient_details->>'pat_comments', '') as pat_comments, p.stat, p.reception_dt, p.urgency, p.has_photo, p.photo_uploaded_dt, false as is_animal, p.reception_no, p.last_edit_dt FROM patients p ORDER BY p.id DESC";
            break;
        case STUDIES:
            querystr = "select studies.id as study_id, studies.patient_id, studies.study_uid, '' as character_set, patients.account_no, patients.full_name, patients.phonogram_name, patients.ideogram_name, patients.birth_date::text, patients.gender,'' as last_study_date,patient_info->'owner_full_name' as owner_full_name, patient_info->'owner_phonogram_name' as owner_phonogram_name,patient_info->'owner_ideogram_name' as owner_ideogram_name, patient_info->'species' as species,patient_info->'bleed' as bleed, false as is_animal, studies.patient_age, studies.accession_no, studies.study_dt, studies.cpt_codes, studies.study_description, studies.orientation, studies.body_part, studies.modalities from studies inner join patients on studies.patient_id = patients.id and studies.no_of_instances > 0 and studies.no_of_series > 0 and study_details is not null ORDER BY studies.id DESC";
            break;
        case DICOMS:
            querystr = "WITH max_ins AS (SELECT study_id , max(study_series_instances.id) AS max_ins_id FROM study_series_instances GROUP BY study_id),Study_status_UNR AS (SELECT status_desc FROM study_status WHERE status_code = 'UNR') SELECT studies.id as study_id, studies.patient_id, studies.study_uid, '' as character_set, patients.account_no, patients.full_name, patients.phonogram_name, patients.ideogram_name, patients.birth_date::text, patients.gender,'' as last_study_date,patient_info->'owner_full_name' as owner_full_name, patient_info->'owner_phonogram_name' as owner_phonogram_name,patient_info->'owner_ideogram_name' as owner_ideogram_name, patient_info->'species' as species,patient_info->'bleed' as bleed, false as is_animal, studies.patient_age, studies.accession_no, studies.study_dt, studies.cpt_codes, studies.study_description, studies.orientation, studies.body_part, studies.modalities, studies.study_details, studies.no_of_series, studies.no_of_instances FROM max_ins INNER JOIN studies ON studies.id = max_ins.study_id INNER JOIN patients ON (studies.patient_id = patients.id AND studies.no_of_instances > 0 AND studies.no_of_series > 0) order by studies.no_of_instances desc";
            break;
        case AUTHENTICATE:
            querystr = "SELECT password FROM users WHERE is_active = true AND has_deleted = false";
            break;
        default:
            querystr = "SELECT display_code,display_description,duration FROM cpt_codes";
            break;
        }
        return querystr;
    }

    private DefaultTableModel getDefaultTableModel(String query) {
        DefaultTableModel dm = new DefaultTableModel();
        try {
            ResultSet rs = statement.executeQuery(query);
            ResultSetMetaData rsmd = rs.getMetaData();
            int cols = rsmd.getColumnCount();
            String s[] = new String[cols];
            for (int i = 0; i < cols; i++) {
                s[i] = rsmd.getColumnName(i + 1);
                dm.addColumn(s[i]);
            }
            Object row[] = new Object[cols];
            while (rs.next()) {
                for (int i = 0; i < cols; i++)
                    row[i] = rs.getString(i + 1);
                dm.addRow(row);
            }
        } catch (java.sql.SQLException e) {
            System.err.println(e);
            System.exit(-1);
        }
        return dm;
    }

    private void setSign(JFrame jf) {
        JPanel jpSignin = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        jpSignin.setPreferredSize(new Dimension(100, 300));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        jpSignin.add(new JLabel("user name: "), gbc);
        jtfUsername = new JTextField(15);
        jtfPassword = new JPasswordField(15);
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        jpSignin.add(jtfUsername, gbc);
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        jpSignin.add(new JLabel("password: "), gbc);
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        jpSignin.add(jtfPassword, gbc);
        JButton jbtnSubmit = new JButton("Login");
        jbtnSubmit.addActionListener(this);
        jbtnSubmit.setActionCommand(EPageMode.SIGNIN.toString());
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        jpSignin.add(jbtnSubmit, gbc);
        jlError = new JLabel("");
        jpSignin.add(jlError);
        jpSignin.setBorder(new LineBorder(Color.GRAY));
        jf.getContentPane().add(jpSignin);
    }

    public void actionPerformed(ActionEvent ae) {
        String actCmd = ae.getActionCommand();
        switch (EPageMode.valueOf(actCmd)) {
        case SIGNIN: {
            if (this.getAuthenticate(this.getQuery(EQuery.AUTHENTICATE.toString())))
                this.showRecords();
            else
                jlError.setText("try again...");
        }
            break;
        case LOGOUT:
                this.showLogout();
            break;
        case RECORDS:
        case FORM:
            break;
        }
    }

    private void showRecords() {
        JFrame jf;
        for (int f = 0; f < jfList.size(); f++) {
            jf = jfList.get(f);
            jf.getContentPane().removeAll();
            JPanel jpTop = new JPanel(new FlowLayout(FlowLayout.RIGHT)),
                    jpBottom = new JPanel(new FlowLayout(FlowLayout.CENTER)),
                    jpContent = new JPanel(new BorderLayout(8, 8));
            JButton jbtnLogout = new JButton("Log out");
            jpTop.add(jbtnLogout);
            jbtnLogout.addActionListener(this);
            jbtnLogout.setActionCommand(EPageMode.LOGOUT.toString());
            jpBottom.add(new JTextField("", 15));
            jpBottom.add(new JButton("Search"));
            JTable jt = new JTable();
            jt.setModel(this.getDefaultTableModel(this.getQuery(EQuery.valueOf(f).toString())));
            TableRowSorter<TableModel> sorter = new TableRowSorter<TableModel>(jt.getModel());
            jt.setRowSorter(sorter);
            JScrollPane jspTable = new JScrollPane(jt);
            jf.setTitle(EQuery.valueOf(f).toString());
            jpContent.add(jpTop, BorderLayout.NORTH);
            jpContent.add(jspTable, BorderLayout.CENTER);
            jpContent.add(jpBottom, BorderLayout.SOUTH);
            jf.getContentPane().add(jpContent);
            jf.revalidate();
            jf.repaint();
        }
    }

    private boolean getAuthenticate(String query) {
        boolean auth = false;
        try {
            ResultSet rs = statement.executeQuery(query + " AND username = '" + jtfUsername.getText() + "'");
            String strPassword = "";
            while (rs.next())
                strPassword = rs.getString("password");
            if (strPassword.length() > 0) {
                BCrypt bcrypt = new BCrypt();
                auth = bcrypt.checkpw(jtfPassword.getText(), strPassword);
            }
        } catch (java.sql.SQLException e) {
            System.err.println(e);
            System.exit(-1);
        }
        return auth;
    }

    public void windowClosing(WindowEvent e) {
        try {
            connection.close();
        } catch (java.sql.SQLException sqle) {
            System.err.println(sqle);
            System.exit(-1);
        }
        //dispose();
        System.exit(0);
    }

    public void windowOpened(WindowEvent e) {
    }

    public void windowActivated(WindowEvent e) {
    }

    public void windowIconified(WindowEvent e) {
    }

    public void windowDeiconified(WindowEvent e) {
    }

    public void windowDeactivated(WindowEvent e) {
    }

    public void windowClosed(WindowEvent e) {
    }

    private void showLogout() {
        JFrame jf;
        for (int f = 0; f < jfList.size(); f++) {
            jf = jfList.get(f);
            jf.setTitle("Authenticate");
            jf.getContentPane().removeAll();
            if (f == 0) this.setSign(jf);
            jf.revalidate();
            jf.repaint();
        }
    }
}
