package com.yangdai.calc.core;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Shell {
    public static String run(String cmd) {
        StringBuilder sb = new StringBuilder();
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"sh", "-c", cmd});
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String l;
            while ((l = br.readLine()) != null) sb.append(l).append("\n");
            p.waitFor();
        } catch (Exception e) {
            sb.append("error: ").append(e.getMessage());
        }
        return sb.toString();
    }
}
