import java.awt.*;
import java.sql.*;
import javax.swing.*;
import javax.swing.table.*;

public class Displays {
    public static void main(String[] arv) {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println(e);
            System.exit(-1);
        }

        try {
            Connection connection = DriverManager.getConnection("jdbc:postgresql://192.168.1.104:5433/KM", "postgres",
                    "1q2w3e4r5t");
            String query = "";
            Statement statement = connection.createStatement();
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice[] gds = ge.getScreenDevices();
            for (int d = 0; d < gds.length; d++) {
                switch(d) {
                    case 0:
                        query = "SELECT p.account_no, p.full_name, p.id as patient_id, p.phonogram_name, p.ideogram_name, p.birth_date::text, p.gender, '' as last_study_date, p.patient_info, nullif(p.patient_details->>'pat_comments', '') as pat_comments, p.stat, p.reception_dt, p.urgency, p.has_photo, p.photo_uploaded_dt, false as is_animal, p.reception_no, p.last_edit_dt FROM patients p";
                    break;
                    case 1:
                        query = "select studies.id as study_id, studies.patient_id, studies.study_uid, '' as character_set, patients.account_no, patients.full_name, patients.phonogram_name, patients.ideogram_name, patients.birth_date::text, patients.gender,'' as last_study_date,patient_info->'owner_full_name' as owner_full_name, patient_info->'owner_phonogram_name' as owner_phonogram_name,patient_info->'owner_ideogram_name' as owner_ideogram_name, patient_info->'species' as species,patient_info->'bleed' as bleed, false as is_animal, studies.patient_age, studies.accession_no, studies.study_dt, studies.cpt_codes, studies.study_description, studies.orientation, studies.body_part, studies.modalities from studies inner join patients on studies.patient_id = patients.id and studies.no_of_instances > 0 and studies.no_of_series > 0 and study_details is not null";
                    break;
                    case 2:
                        query = "WITH max_ins AS (SELECT study_id , max(study_series_instances.id) AS max_ins_id FROM study_series_instances GROUP BY study_id),Study_status_UNR AS (SELECT status_desc FROM study_status WHERE status_code = 'UNR') SELECT studies.id as study_id, studies.patient_id, studies.study_uid, '' as character_set, patients.account_no, patients.full_name, patients.phonogram_name, patients.ideogram_name, patients.birth_date::text, patients.gender,'' as last_study_date,patient_info->'owner_full_name' as owner_full_name, patient_info->'owner_phonogram_name' as owner_phonogram_name,patient_info->'owner_ideogram_name' as owner_ideogram_name, patient_info->'species' as species,patient_info->'bleed' as bleed, false as is_animal, studies.patient_age, studies.accession_no, studies.study_dt, studies.cpt_codes, studies.study_description, studies.orientation, studies.body_part, studies.modalities, studies.study_details, studies.no_of_series, studies.no_of_instances FROM max_ins INNER JOIN studies ON studies.id = max_ins.study_id INNER JOIN patients ON (studies.patient_id = patients.id AND studies.no_of_instances > 0 AND studies.no_of_series > 0) order by studies.no_of_instances desc";
                    break;
                    default:
                        query = "SELECT display_code,display_description,duration FROM cpt_codes";
                    break;
                }
                ResultSet rs = statement.executeQuery(query);
                ResultSetMetaData rsmd = rs.getMetaData();
                DefaultTableModel dm = new DefaultTableModel();
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
                GraphicsDevice gd = gds[d];
                GraphicsConfiguration[] gc = gd.getConfigurations();
                for (int c = 0; c < gc.length; c++) {
                    JFrame jf = new JFrame(gd.getDefaultConfiguration());
                    jf.setExtendedState(JFrame.MAXIMIZED_BOTH);
                    jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                    Rectangle rect = gc[c].getBounds();
                    jf.setLocation(rect.x, rect.y);
                    //jf.setLayout(new FlowLayout());
                    JTable jt = new JTable();
                    jt.setModel(dm);
                    JScrollPane sp = new JScrollPane(jt);
                    jf.add(sp);
                    //jf.add(new JLabel("i am home", JLabel.CENTER));
                    jf.setVisible(true);
                }
            }
            connection.close();
        } catch (java.sql.SQLException e) {
            System.err.println(e);
            System.exit(-1);
        }
    }
}
