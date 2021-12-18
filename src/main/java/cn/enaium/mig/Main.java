package cn.enaium.mig;

import com.formdev.flatlaf.FlatDarculaLaf;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;

/**
 * @author Enaium
 */
public class Main extends JFrame {
    public static void main(String[] args) {
        FlatDarculaLaf.setup();
        try {
            new Main().setVisible(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Main() throws IOException {
        super("Maven Index Generator");

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(500, 100);
        setLocationRelativeTo(getOwner());
        setLayout(new BorderLayout());

        final String[] head = {new String(Objects.requireNonNull(this.getClass().getResourceAsStream("/head.html")).readAllBytes(), StandardCharsets.UTF_8)};
        final String[] body = {new String(Objects.requireNonNull(this.getClass().getResourceAsStream("/body.html")).readAllBytes(), StandardCharsets.UTF_8)};
        final String[] list = {new String(Objects.requireNonNull(this.getClass().getResourceAsStream("/list.html")).readAllBytes(), StandardCharsets.UTF_8)};
        final String[] template = {new String(Objects.requireNonNull(this.getClass().getResourceAsStream("/template.html")).readAllBytes(), StandardCharsets.UTF_8)};

        File headLocal = new File(System.getProperty("user.dir"), "head.html");
        if (!headLocal.exists()) {
            Files.writeString(headLocal.toPath(), head[0]);
        }

        File bodyLocal = new File(System.getProperty("user.dir"), "body.html");
        if (!bodyLocal.exists()) {
            Files.writeString(bodyLocal.toPath(), body[0]);
        }

        File listTemplateLocal = new File(System.getProperty("user.dir"), "list.html");
        if (!listTemplateLocal.exists()) {
            Files.writeString(listTemplateLocal.toPath(), list[0]);
        }

        File templateLocal = new File(System.getProperty("user.dir"), "template.html");
        if (!templateLocal.exists()) {
            Files.writeString(templateLocal.toPath(), template[0]);
        }

        JLabel jLabel = new JLabel("Maven Local Path");
        add(jLabel, BorderLayout.WEST);
        JTextField jTextField = new JTextField();
        String url = System.getenv("MAVEN_URL");
        if (url != null) {
            jTextField.setText(System.getenv("MAVEN_URL"));
        }
        add(jTextField, BorderLayout.CENTER);
        JButton jButton = new JButton("Generator");
        jButton.addActionListener(e -> {
            try {
                Files.walkFileTree(new File(jTextField.getText()).toPath(), new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                        head[0] = Files.readString(headLocal.toPath(), StandardCharsets.UTF_8);
                        body[0] = Files.readString(bodyLocal.toPath(), StandardCharsets.UTF_8);
                        list[0] = Files.readString(listTemplateLocal.toPath(), StandardCharsets.UTF_8);
                        template[0] = Files.readString(templateLocal.toPath(), StandardCharsets.UTF_8);
                        String relativePath = getRelativePath(dir.toFile(), jTextField.getText());
                        if (!isFilter(relativePath)) {
                            head[0] = head[0].replace("{{ title }}", "Index of " + relativePath.replace("\\", "/"));
                            body[0] = body[0].replace("{{ name }}", "Index of " + relativePath.replace("\\", "/"));
                            File absolutePath = new File(jTextField.getText(), relativePath);

                            File[] files = absolutePath.listFiles();
                            if (absolutePath.exists() && files != null) {
                                StringBuilder lists = new StringBuilder();
                                if (!relativePath.equals("")) {
                                    lists.append(list[0].replace("{{ name }}", "../"));
                                }
                                for (File file : files) {
                                    if (!isFilter(file.getName())) {
                                        lists.append(list[0].replace("{{ name }}", file.getName()));
                                    }
                                }
                                Files.writeString(new File(absolutePath, "index.html").toPath(), template[0].replace("{{ head }}", head[0]).replace("{{ body }}", body[0].replace("{{ list }}", lists.toString())));
                            }
                        }
                        return super.preVisitDirectory(dir, attrs);
                    }
                });
                JOptionPane.showMessageDialog(null, "Generator Success");
            } catch (IOException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null, ex, "ERROR", JOptionPane.ERROR_MESSAGE);
            }
        });
        add(jButton, BorderLayout.SOUTH);
    }

    public String getRelativePath(File file, String current) {
        String relativePath = file.getAbsolutePath().substring(current.length());
        if (!relativePath.equals("") && (relativePath.charAt(0) == '/' || relativePath.charAt(0) == '\\')) {
            relativePath = relativePath.substring(1);
        }
        return relativePath;
    }

    public boolean isFilter(String text) {
        return text.matches("index.html|CNAME|.git.*");
    }
}
