package com.ray.myclaw;

public final class Probe {
    public static void main(String[] args) {
        TranscriptView view = new TranscriptView();
        view.appendUser("first prompt");
        String text = view.text();
        System.out.println("LEN=" + text.length());
        System.out.println("TEXT=[" + text + "]");
        System.out.println("CONTAINS=" + text.contains("You:\nfirst prompt"));
        for (int i = 0; i < text.length(); i++) {
            System.out.println(i + ": " + (int) text.charAt(i) + " '" + text.charAt(i) + "'");
        }
    }
}
