package me.surge.test;

public enum JEnumTest {

    MEMBER_ONE(3),
    MEMBER_TWO(5);

    public final int a;

    JEnumTest(int a) {
        this.a = a;
    }

    public void output() {
        System.out.println("wowza " + a);
    }

}
