package com.ray.myclaw;

import java.nio.charset.StandardCharsets;

final class CommandTestProgram {
    private CommandTestProgram() {
    }

    public static void main(String[] args) throws Exception {
        switch (args[0]) {
            case "stdout" -> System.out.write(args[1].getBytes(StandardCharsets.UTF_8));
            case "stderr" -> System.err.write(args[1].getBytes(StandardCharsets.UTF_8));
            case "exit" -> System.exit(Integer.parseInt(args[1]));
            case "args" -> System.out.write(args[1].getBytes(StandardCharsets.UTF_8));
            case "stdin" -> System.out.write(System.in.readAllBytes());
            case "sleep" -> Thread.sleep(Long.parseLong(args[1]));
            case "largeBoth" -> writeLargeBoth(Integer.parseInt(args[1]));
            case "unicode" -> System.out.write("snowman \u2603 kanji \u6f22 emoji \uD83D\uDE80".getBytes(StandardCharsets.UTF_8));
            default -> throw new IllegalArgumentException("unknown mode " + args[0]);
        }
    }

    private static void writeLargeBoth(int length) throws InterruptedException {
        Thread stdout = new Thread(() -> writeRepeated(System.out, 'o', length));
        Thread stderr = new Thread(() -> writeRepeated(System.err, 'e', length));
        stdout.start();
        stderr.start();
        stdout.join();
        stderr.join();
    }

    private static void writeRepeated(java.io.PrintStream stream, char character, int length) {
        byte[] bytes = String.valueOf(character).repeat(length).getBytes(StandardCharsets.UTF_8);
        stream.writeBytes(bytes);
    }
}
