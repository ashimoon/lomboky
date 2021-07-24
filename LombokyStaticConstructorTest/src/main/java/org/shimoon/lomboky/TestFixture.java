package org.shimoon.lomboky;

@StaticConstructor
public class TestFixture {
    public String sayHello() {
        return "hellojello";
    }

    public static void main(final String[] args) {
        if (!"hellojello".equals(TestFixture.of().sayHello())) {
            throw new IllegalStateException("unexpected output");
        }
        System.out.println("success");
    }
}
