import java.awt.*;
import javax.swing.*;
import java.util.*;
import java.sql.*;
import javax.swing.table.*;
import java.lang.*;
import java.awt.event.*;

enum EPage {
    SIGNIN, RECORDS, FORM
};

enum EQuery {
    AUTHENTICATE, PATIENTS, STUDIES, DICOMS
};

public class EX implements ActionListener {
    private Connection connection;
    EPage epage;
    JFrame jf;
    private JTextField jtfUsername, jtfPassword;
    private JLabel jlError;

    public static void main(String[] args) {
        EX exobj = new EX();
        exobj.checkDB();
        exobj.setFrame();
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
            connection.close();
        } catch (java.sql.SQLException e) {
            System.err.println(e);
            System.exit(-1);
        }
    }

    private void setFrame() {
        jf = new JFrame("Authenticate");
        jf.setExtendedState(JFrame.MAXIMIZED_BOTH);
        jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setSign();
        jf.setVisible(true);
    }

    private String getQuery(String query) {
        String querystr;
        switch (EQuery.valueOf(query)) {
        case PATIENTS:
            querystr = "SELECT p.account_no, p.full_name, p.id as patient_id, p.phonogram_name, p.ideogram_name, p.birth_date::text, p.gender, '' as last_study_date, p.patient_info, nullif(p.patient_details->>'pat_comments', '') as pat_comments, p.stat, p.reception_dt, p.urgency, p.has_photo, p.photo_uploaded_dt, false as is_animal, p.reception_no, p.last_edit_dt FROM patients p";
            break;
        case STUDIES:
            querystr = "select studies.id as study_id, studies.patient_id, studies.study_uid, '' as character_set, patients.account_no, patients.full_name, patients.phonogram_name, patients.ideogram_name, patients.birth_date::text, patients.gender,'' as last_study_date,patient_info->'owner_full_name' as owner_full_name, patient_info->'owner_phonogram_name' as owner_phonogram_name,patient_info->'owner_ideogram_name' as owner_ideogram_name, patient_info->'species' as species,patient_info->'bleed' as bleed, false as is_animal, studies.patient_age, studies.accession_no, studies.study_dt, studies.cpt_codes, studies.study_description, studies.orientation, studies.body_part, studies.modalities from studies inner join patients on studies.patient_id = patients.id and studies.no_of_instances > 0 and studies.no_of_series > 0 and study_details is not null";
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
            connection = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:5432/APR05", "postgres",
                    "1q2w3e4r5t");
            Statement statement = connection.createStatement();
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
            connection.close();
        } catch (java.sql.SQLException e) {
            System.err.println(e);
            System.exit(-1);
        }
        return dm;
    }

    private void setSign() {
        JPanel jpSignin = new JPanel(new FlowLayout(FlowLayout.CENTER));
        jpSignin.setPreferredSize(new Dimension(100, 300));
        jpSignin.add(new JLabel("user name: "));
        jtfUsername = new JTextField(15);
        jtfPassword = new JPasswordField(15);
        jpSignin.add(jtfUsername);
        jpSignin.add(new JLabel("password: "));
        jpSignin.add(jtfPassword);
        JButton jbtnSubmit = new JButton("Login");
        jbtnSubmit.addActionListener(this);
        jbtnSubmit.setActionCommand(EPage.SIGNIN.toString());
        jpSignin.add(jbtnSubmit);
        jlError = new JLabel("");
        jpSignin.add(jlError);
        jf.add(jpSignin);
    }

    public void actionPerformed(ActionEvent ae) {
        String actCmd = ae.getActionCommand();
        switch (EPage.valueOf(actCmd)) {
        case SIGNIN: {
            if (this.getAuthenticate(this.getQuery(EQuery.AUTHENTICATE.toString())))
                this.showRecords();
            else
                jlError.setText("try again...");
        }
            break;
        case RECORDS:
        case FORM:
            break;
        }
    }

    private void showRecords() {
        jf.getContentPane().removeAll();
        JTable jt = new JTable();
        jt.setModel(this.getDefaultTableModel(this.getQuery(EQuery.PATIENTS.toString())));
        JScrollPane sp = new JScrollPane(jt);
        jf.setTitle("Patients");
        jf.getContentPane().add(sp);
        jf.revalidate();
        jf.repaint();
    }

    private boolean getAuthenticate(String query) { 
        boolean auth = false;
        try {
            connection = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:5432/APR05", "postgres",
                    "1q2w3e4r5t");
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery(query + " AND username = '" + jtfUsername.getText() + "'");
            String strPassword = "";
            while (rs.next()) 
                strPassword = rs.getString("password");
            connection.close();
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
}
