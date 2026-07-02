package org.jboss.set.channel.cli;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.assertj.core.api.Assertions.assertThat;

public class SortVersionsCommandTestCase {

    @Test
    public void testSortSimpleVersions() throws Exception {
        String input = "1.0.2 1.0.10 1.0.1";
        String output = runCommand(input);
        
        assertThat(output).isEqualTo("1.0.1\n1.0.2\n1.0.10\n");
    }

    @Test
    public void testSortVersionsWithQualifiers() throws Exception {
        String input = "1.0.0.Final 1.0.0.RC1 2.0.0.Alpha1 1.0.0.Beta1";
        String output = runCommand(input);
        
        assertThat(output).isEqualTo("1.0.0.Beta1\n1.0.0.Final\n1.0.0.RC1\n2.0.0.Alpha1\n");
    }

    @Test
    public void testSortVersionsWithEpoch() throws Exception {
        String input = "1:1.0.0 0:2.0.0 2:0.5.0";
        String output = runCommand(input);
        
        assertThat(output).isEqualTo("0:2.0.0\n1:1.0.0\n2:0.5.0\n");
    }

    @Test
    public void testSortVersionsMultiline() throws Exception {
        String input = "1.0.2\n1.0.10\n1.0.1";
        String output = runCommand(input);
        
        assertThat(output).isEqualTo("1.0.1\n1.0.2\n1.0.10\n");
    }

    @Test
    public void testSortVersionsMixedWhitespace() throws Exception {
        String input = "1.0.2\t1.0.10  1.0.1\n1.0.5";
        String output = runCommand(input);
        
        assertThat(output).isEqualTo("1.0.1\n1.0.2\n1.0.5\n1.0.10\n");
    }

    @Test
    public void testSortEmptyInput() throws Exception {
        String input = "";
        String output = runCommand(input);
        
        assertThat(output).isEmpty();
    }

    @Test
    public void testSortSingleVersion() throws Exception {
        String input = "1.0.0";
        String output = runCommand(input);
        
        assertThat(output).isEqualTo("1.0.0\n");
    }

    @Test
    public void testSortRedHatVersions() throws Exception {
        String input = "1.0.0.redhat-00001 1.0.0.redhat-00010 1.0.0.redhat-00002";
        String output = runCommand(input);
        
        assertThat(output).isEqualTo("1.0.0.redhat-00001\n1.0.0.redhat-00002\n1.0.0.redhat-00010\n");
    }

    @Test
    public void testSortComplexVersions() throws Exception {
        String input = "3.0.0 2.1.5 2.1.10 1.9.0 2.0.0";
        String output = runCommand(input);
        
        assertThat(output).isEqualTo("1.9.0\n2.0.0\n2.1.5\n2.1.10\n3.0.0\n");
    }

    private String runCommand(String input) throws Exception {
        // Redirect stdin
        ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes());
        System.setIn(in);
        
        // Redirect stdout
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(out));
        
        try {
            SortVersionsCommand command = new SortVersionsCommand();
            command.call();
            return out.toString();
        } finally {
            // Restore original streams
            System.setOut(originalOut);
            System.setIn(System.in);
        }
    }
}
