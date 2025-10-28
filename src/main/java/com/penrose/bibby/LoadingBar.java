package com.penrose.bibby;

public class LoadingBar {

    public static void showProgressBar(String taskName, int totalSteps, int delayMs) throws InterruptedException {
        System.out.println(taskName);
        for (int i = 0; i <= totalSteps; i++) {
            int percent = (i * 100) / totalSteps;
            String bar = "\uD83D\uDFE9".repeat(i) + " ".repeat(totalSteps - i);
            System.out.print("\r[" + bar + "] " + percent + "%");
            Thread.sleep(delayMs);
        }
        System.out.println("\n✅ Done!");
    }
}