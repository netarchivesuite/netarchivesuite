package dk.netarkivet.monitor.webinterface;

import java.util.Locale;

import junit.framework.TestCase;

/**
 * Created by IntelliJ IDEA. User: csr Date: 10/5/11 Time: 11:55 AM To change
 * this template use File | Settings | File Templates.
 */
public class JMXSummaryUtilsTester extends TestCase {

    public void testGenerateMessage() {
        String input = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Aliquam volutpat euismod aliquet. Nullam vestibulum mollis arcu, quis laoreet nibh aliquet et. In at ligula pellentesque magna placerat luctus. Donec mauris nibh, lacinia non feugiat quis, dapibus id orci. Suspendisse sollicitudin suscipit sodales. Mauris interdum consectetur nunc sed interdum. Nulla facilisi. Quisque urna lectus, tempor ut feugiat sit amet, congue eget lectus. Duis eget interdum turpis. Morbi turpis arcu, venenatis ac venenatis nec, pretium ac tellus. Fusce condimentum iaculis sem. Cras eros dui, imperdiet vitae faucibus feugiat, pellentesque eu quam. In dignissim facilisis sollicitudin. Cras tincidunt arcu at lectus tincidunt a porta lorem accumsan. Pellentesque porta, est at viverra sagittis, est elit congue lorem, feugiat lobortis tellus nisl in augue.";
        String output = JMXSummaryUtils.generateMessage(input, Locale.getDefault());
        System.out.println(output);
        assertTrue("Should have split String into many lines, not " + output.split("\n").length, output.split("\n").length > 10);
    }

   public void testGenerateMessageWithForcing() {
        String input = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Aliquam volutpat euismod aliquet. Nullam vestibulum mollis arcu, quis laoreet nibh aliquet et. In at ligula aaaaaaaaaaaaaaaaaabbbbbbbbbbbbbbbbbbcccccccccccccccccdddddddddddddddddddddeeeeeeeeeeeeeeepellentesque magna placerat luctus. Donec mauris nibh, lacinia non feugiat quis, dapibus id orci. Suspendisse sollicitudin suscipit sodales. Mauris interdum consectetur nunc sed interdum. Nulla facilisi. Quisque urna lectus, tempor ut feugiat sit amet, congue eget lectus. Duis eget interdum turpis. Morbi turpis arcu, venenatis ac venenatis nec, pretium ac tellus. Fusce condimentum iaculis sem. Cras eros dui, imperdiet vitae faucibus feugiat, pellentesque eu quam. In dignissim facilisis sollicitudin. Cras tincidunt arcu at lectus tincidunt a porta lorem accumsan. Pellentesque porta, est at viverra sagittis, est elit congue lorem, feugiat lobortis tellus nisl in augue.";
        String output = JMXSummaryUtils.generateMessage(input, Locale.getDefault());
        System.out.println(output);
        assertTrue("Should have split String into many lines, not " + output.split("\n").length, output.split("\n").length > 10);
    }
}
