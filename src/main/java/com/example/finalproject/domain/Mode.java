package com.example.finalproject.domain;

public enum Mode {
    일반(1),
    바보(2);

    private int num;

    Mode(int num){
        this.num = num;
    }

    public static Mode modeName(int num){
        switch (num){
            case 1:
                return 일반;
            case 2:
                return 바보;
        }
        return null;
    }

}
