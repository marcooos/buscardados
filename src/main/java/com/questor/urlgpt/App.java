package com.questor.urlgpt;

import javax.swing.SwingUtilities;

import com.questor.urlgpt.service.MarkdownFetcherApp;

/**
 * Hello world!
 *
 */
public class App {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MarkdownFetcherApp().setVisible(true));
    }
}
