package com.questor.urlgpt.service;

/*
 * MarkdownFetcherApp.java
 * ------------------------------------------------------------
 * App desktop (Swing, Java 17+) para colar uma lista de URLs,
 * baixar o conteúdo principal de cada página e gerar UM arquivo
 * Markdown (.md) consolidado para envio manual ao Assistente.
 *
 * Requisitos (Maven):
 * <dependency>
 *   <groupId>org.jsoup</groupId>
 *   <artifactId>jsoup</artifactId>
 *   <version>1.17.2</version>
 * </dependency>
 *
 * Observações:
 *  - Suporta páginas HTML públicas. Para PDF, recomenda-se baixar manualmente e
 *    converter a texto/Markdown com outra ferramenta antes (ou apenas incluir o link).
 *  - Heurística simples para extrair conteúdo: tenta <main>, <article>, [role=main];
 *    remove nav/header/footer/aside e elementos comuns de layout/ads; preserva títulos,
 *    parágrafos e listas básicas em Markdown.
 *  - Não requer API key da OpenAI. A ideia é gerar o .md localmente e subir como "file".
 */

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MarkdownFetcherApp extends JFrame {

    private final JTextArea urlsArea = new JTextArea(10, 60);
    private final JTextField userAgentField = new JTextField("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 Chrome/124 Safari/537.36");
    private final JSpinner timeoutSpinner = new JSpinner(new SpinnerNumberModel(20, 5, 120, 5)); // segundos
    private final JCheckBox includeMeta = new JCheckBox("Incluir metadados (título, data, fonte)", true);
    private final JCheckBox addTOC = new JCheckBox("Gerar Sumário (TOC)", true);
    private final JTextField docTitleField = new JTextField("Fontes para o Assistente");
    private final JTextArea logArea = new JTextArea(8, 60);

    public MarkdownFetcherApp() {
        super("Markdown Fetcher – URLs → .md");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new EmptyBorder(12,12,12,12));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6,6,6,6);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTHWEST;

        int row = 0;

        // Título do documento
        c.gridx = 0; c.gridy = row; c.weightx = 0; form.add(new JLabel("Título do .md:"), c);
        c.gridx = 1; c.gridy = row; c.weightx = 1; form.add(docTitleField, c); row++;

        // URLs
        c.gridx = 0; c.gridy = row; c.weightx = 0; form.add(new JLabel("URLs (uma por linha):"), c);
        urlsArea.setLineWrap(true);
        urlsArea.setWrapStyleWord(true);
        JScrollPane spUrls = new JScrollPane(urlsArea);
        c.gridx = 1; c.gridy = row; c.weightx = 1; c.fill = GridBagConstraints.BOTH; c.weighty = 1; form.add(spUrls, c); row++;
        c.fill = GridBagConstraints.HORIZONTAL; c.weighty = 0;

        // User-Agent e Timeout
        JPanel uaPanel = new JPanel(new GridLayout(1,2,8,0));
        JPanel left = new JPanel(new BorderLayout(6,0));
        left.add(new JLabel("User-Agent:"), BorderLayout.WEST);
        left.add(userAgentField, BorderLayout.CENTER);
        JPanel right = new JPanel(new BorderLayout(6,0));
        right.add(new JLabel("Timeout (s):"), BorderLayout.WEST);
        right.add(timeoutSpinner, BorderLayout.CENTER);
        uaPanel.add(left); uaPanel.add(right);
        c.gridx = 0; c.gridy = row; c.weightx = 0; form.add(new JLabel("Opções de rede:"), c);
        c.gridx = 1; c.gridy = row; c.weightx = 1; form.add(uaPanel, c); row++;

        // Opções
        JPanel opts = new JPanel(new FlowLayout(FlowLayout.LEFT));
        opts.add(includeMeta); opts.add(addTOC);
        c.gridx = 0; c.gridy = row; c.weightx = 0; form.add(new JLabel("Opções:"), c);
        c.gridx = 1; c.gridy = row; c.weightx = 1; form.add(opts, c); row++;

        // Botões
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnPaste = new JButton("Colar URLs");
        JButton btnGenerate = new JButton("Gerar .md");
        JButton btnClear = new JButton("Limpar");
        buttons.add(btnPaste); buttons.add(btnClear); buttons.add(btnGenerate);
        c.gridx = 0; c.gridy = row; c.gridwidth = 2; form.add(buttons, c); row++;

        // Log
        logArea.setEditable(false);
        JScrollPane spLog = new JScrollPane(logArea);
        spLog.setBorder(BorderFactory.createTitledBorder("Log"));

        add(form, BorderLayout.CENTER);
        add(spLog, BorderLayout.SOUTH);

        // Actions
        btnPaste.addActionListener(this::onPaste);
        btnClear.addActionListener(e -> urlsArea.setText(""));
        btnGenerate.addActionListener(this::onGenerate);

        setSize(900, 650);
        setLocationRelativeTo(null);
    }

    private void onPaste(ActionEvent e) {
        try {
            String s = Toolkit.getDefaultToolkit().getSystemClipboard()
                    .getData(java.awt.datatransfer.DataFlavor.stringFlavor).toString();
            urlsArea.append((urlsArea.getText().isBlank() ? "" : "\n") + s.trim());
        } catch (Exception ex) {
            log("Falha ao colar: " + ex.getMessage());
        }
    }

    private void onGenerate(ActionEvent e) {
        String[] lines = urlsArea.getText().split("\r?\n");
        List<String> urls = new ArrayList<>();
        for (String l : lines) {
            String u = l.trim();
            if (!u.isBlank()) urls.add(u);
        }
        if (urls.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Informe ao menos uma URL.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File(safeFileName(docTitleField.getText()) + ".md"));
        int res = fc.showSaveDialog(this);
        if (res != JFileChooser.APPROVE_OPTION) return;
        File out = fc.getSelectedFile();

        new Thread(() -> {
            setControlsEnabled(false);
            try {
                String md = buildMarkdown(urls);
                try (BufferedWriter w = Files.newBufferedWriter(out.toPath(), StandardCharsets.UTF_8)) {
                    w.write(md);
                }
                log("✔ Gerado: " + out.getAbsolutePath());
                int copy = JOptionPane.showConfirmDialog(this,
                        "Arquivo gerado. Copiar caminho para a área de transferência?",
                        "Concluído", JOptionPane.YES_NO_OPTION);
                if (copy == JOptionPane.YES_OPTION) {
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(out.getAbsolutePath()), null);
                }
            } catch (Exception ex) {
                log("✖ Erro: " + ex.getMessage());
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            } finally {
                setControlsEnabled(true);
            }
        }).start();
    }

    private void setControlsEnabled(boolean enabled) {
        SwingUtilities.invokeLater(() -> {
            for (Component comp : ((JPanel)getContentPane().getComponent(0)).getComponents()) comp.setEnabled(enabled);
        });
    }

    private String buildMarkdown(List<String> urls) {
        StringBuilder md = new StringBuilder();
        String title = docTitleField.getText().isBlank() ? "Fontes" : docTitleField.getText().trim();
        md.append("# ").append(escapeMd(title)).append("\n\n");
        if (includeMeta.isSelected()) {
            String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            md.append("> Documento gerado em ").append(ts).append("\n\n");
        }

        // Coleta títulos para TOC
        List<String> sectionTitles = new ArrayList<>();
        List<String> sectionAnchors = new ArrayList<>();

        int i = 1;
        for (String u : urls) {
            try {
                log("Baixando: " + u);
                Document doc = fetch(u);
                String pageTitle = bestTitle(doc);
                String anchor = toAnchor(pageTitle, i);
                sectionTitles.add(pageTitle);
                sectionAnchors.add(anchor);
            } catch (Exception ex) {
                sectionTitles.add("(Falha) " + u);
                sectionAnchors.add(toAnchor("falha-"+i, i));
            }
            i++;
        }

        if (addTOC.isSelected()) {
            md.append("## Sumário\n\n");
            for (int j = 0; j < sectionTitles.size(); j++) {
                md.append("- [").append(escapeMd(sectionTitles.get(j))).append("](#").append(sectionAnchors.get(j)).append(")\n");
            }
            md.append("\n");
        }

        i = 1;
        for (String u : urls) {
            try {
                Document doc = fetch(u);
                String pageTitle = bestTitle(doc);
                String anchor = toAnchor(pageTitle, i);
                md.append("<a id=\"").append(anchor).append("\"></a>\n");
                md.append("## ").append(escapeMd(pageTitle)).append("\n\n");
                if (includeMeta.isSelected()) {
                    md.append("> Fonte: ").append(u).append("\n\n");
                }
                String body = extractMainMarkdown(doc, u);
                md.append(body).append("\n\n");
            } catch (Exception ex) {
                md.append("## Falha ao obter conteúdo\n\n> URL: ").append(u).append("\n\n");
                md.append("Erro: ").append(escapeMd(ex.getMessage())).append("\n\n");
            }
            i++;
        }
        return md.toString();
    }

    private Document fetch(String url) throws IOException {
        int timeoutMs = ((Number) timeoutSpinner.getValue()).intValue() * 1000;
        return Jsoup.connect(url)
                .timeout(timeoutMs)
                .userAgent(userAgentField.getText())
                .followRedirects(true)
                .get();
    }

    private static String bestTitle(Document doc) {
        String t = doc.title();
        if (t == null || t.isBlank()) {
            Elements og = doc.select("meta[property=og:title], meta[name=twitter:title]");
            if (!og.isEmpty()) t = og.first().attr("content");
        }
        return (t == null || t.isBlank()) ? "Sem título" : t.trim();
    }

    private String extractMainMarkdown(Document doc, String baseUrl) {
        // Remove ruídos
        doc.select("script, style, noscript").forEach(Element::remove);
        doc.select("nav, header, footer, aside, .sidebar, .menu, .navbar, .breadcrumbs, .advert, .ads, .cookie, .cookies, .share, .social")
                .forEach(Element::remove);

        Element root = firstNonNull(
                doc.selectFirst("main .theme-doc-markdown"),
                doc.selectFirst(".theme-doc-markdown"),
                doc.selectFirst("article.markdown"),
                doc.selectFirst(".markdown"),
                doc.selectFirst(".md-content"),
                doc.selectFirst(".md-typeset"),
                doc.selectFirst("[role=main]"),
                doc.selectFirst("main"),
                doc.body()
        );
        if (root == null) return "(Conteúdo não encontrado)";

        StringBuilder sb = new StringBuilder();
        for (Element el : root.children()) {
            String tag = el.tagName().toLowerCase();
            switch (tag) {
                case "h1": sb.append("# ").append(escapeMd(el.text())).append("\n\n"); break;
                case "h2": sb.append("## ").append(escapeMd(el.text())).append("\n\n"); break;
                case "h3": sb.append("### ").append(escapeMd(el.text())).append("\n\n"); break;
                case "h4": sb.append("#### ").append(escapeMd(el.text())).append("\n\n"); break;
                case "h5": sb.append("##### ").append(escapeMd(el.text())).append("\n\n"); break;
                case "h6": sb.append("###### ").append(escapeMd(el.text())).append("\n\n"); break;
                case "p": sb.append(paragraphWithLinks(el, baseUrl)).append("\n\n"); break;
                case "ul": sb.append(listToMd(el, false, baseUrl)).append("\n"); break;
                case "ol": sb.append(listToMd(el, true, baseUrl)).append("\n"); break;
                case "pre": sb.append("````\n").append(el.text()).append("\n````\n\n"); break;
                case "table": sb.append("\n> [Tabela omitida na conversão simples] \n\n"); break;
                default:
                    // Parágrafos ou seções genéricas
                    if (el.selectFirst("p") != null) {
                        for (Element p : el.select("p")) sb.append(paragraphWithLinks(p, baseUrl)).append("\n\n");
                    }
            }
        }
        return sb.toString().trim();
    }

    private String listToMd(Element list, boolean ordered, String baseUrl) {
        StringBuilder sb = new StringBuilder();
        int idx = 1;
        for (Element li : list.select("> li")) {
            String content = inlineToMd(li, baseUrl);
            if (ordered) sb.append(idx++).append(". ").append(content).append("");
            else sb.append("- ").append(content).append("");
        }
        return sb.toString();
    }

    private String paragraphWithLinks(Element p, String baseUrl) {
        return inlineToMd(p, baseUrl);
    }

    private String inlineToMd(Element el, String baseUrl) {
        StringBuilder sb = new StringBuilder();
        for (org.jsoup.nodes.Node node : el.childNodes()) {
            if (node instanceof org.jsoup.nodes.TextNode) {
                org.jsoup.nodes.TextNode tn = (org.jsoup.nodes.TextNode) node;
                sb.append(escapeMd(tn.text()));
            } else if (node instanceof Element) {
                Element child = (Element) node;
                String tag = child.tagName().toLowerCase();
                switch (tag) {
                    case "a": {
                        String href = child.absUrl("href");
                        if (href == null || href.isBlank()) href = child.attr("href");
                        String txt = child.text().isBlank() ? href : child.text();
                        sb.append("[").append(escapeMd(txt)).append("](").append(href).append(")");
                        break;
                    }
                    case "strong": case "b":
                        sb.append("**").append(inlineToMd(child, baseUrl)).append("**");
                        break;
                    case "em": case "i":
                        sb.append("_").append(inlineToMd(child, baseUrl)).append("_");
                        break;
                    case "code":
                        sb.append("'").append(child.text().replace("'", "\'")).append("'");
                        break;
                    case "br":
                        sb.append("<br>");
                        break;
                    default:
                        sb.append(inlineToMd(child, baseUrl));
                }
            }
        }
        return sb.toString().trim();
    }

    private static String escapeMd(String s) {
        if (s == null) return "";
        // Escape básico de caracteres Markdown comuns
        return s.replace("\\", "\\\\")
                .replace("*", "\\*")
                .replace("_", "\\_")
                .replace("[", "\\[")
                .replace("]", "\\]")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace("#", "\\#")
                .replace("`", "\\`");
    }

    private static String safeFileName(String s) {
        String base = Objects.requireNonNullElse(s, "documento").trim();
        if (base.isBlank()) base = "documento";
        return base.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private static Element firstNonNull(Element... els) {
        for (Element e : els) if (e != null) return e; return null;
    }

    private void log(String msg) { SwingUtilities.invokeLater(() -> logArea.append(msg + "\n")); }
    private static String toAnchor(String title, int index) {
        String base = title.toLowerCase().replaceAll("[^a-z0-9 ]", "").replaceAll(" +", "-");
        if (base.isBlank()) base = "section-" + index;
        return base;
    }   
}

