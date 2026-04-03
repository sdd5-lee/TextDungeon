package com.textdungeon.model;

import com.google.gson.Gson;

public class Monster {
    private String name;
    private String id;
    private int hp;
    private int maxHp;
    private int attack;

    private Monster() {}
    public static Monster createFromJson(String json) {
        return new Gson().fromJson(json, Monster.class);
    }

    public void takeDamage(int damage) {
        this.hp -= damage;
        if (this.hp < 0) this.hp = 0;
    }

    public boolean isDead() {
        return this.hp <= 0;
    }

    public String getName() { return name; }
    public int getHp() { return hp; }
    public int getMaxHp() { return maxHp; }
    public int getAttack() { return attack; }

    public String getId() { return id; }
}