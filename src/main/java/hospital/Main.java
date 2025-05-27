package main.java.hospital;

import main.java.hospital.menu.MainMenu;

public class
Main {
    public static void main(String[] args) {
        MainMenu.getInstance().init();
        MainMenu.getInstance().show();
    }
}
