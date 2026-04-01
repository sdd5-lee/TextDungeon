package com.textdungeon.player;

public class Magic {
    private int magic_count;
    private int magicDamage;

    public Magic(int magic_count,int wisdom){
        this.magic_count = magic_count;
        this.magicDamage = magic_count * 10;
    }


    public void setMagic_count(int magic_count) {this.magic_count = magic_count;}
    public void setMagicDamage(int magicDamage) {
        this.magicDamage = magicDamage;
    }

    public int getMagic_count() {return magic_count;}
    public int getMagicDamage() {
        return magicDamage;
    }

    public int cast(){

        return magicDamage;
    }

}
