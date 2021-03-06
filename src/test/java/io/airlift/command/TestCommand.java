/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.airlift.command;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.airlift.units.Duration;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static io.airlift.concurrent.Threads.daemonThreadsNamed;
import static io.airlift.testing.EquivalenceTester.equivalenceTester;
import static java.util.concurrent.Executors.newCachedThreadPool;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotSame;

public class TestCommand
{
    private ExecutorService executor;

    @BeforeClass
    public void setUp()
            throws Exception
    {
        executor = newCachedThreadPool(daemonThreadsNamed("process-input-reader-%s"));
    }

    @AfterClass
    public void tearDown()
            throws Exception
    {
        executor.shutdownNow();
    }

    @Test
    public void buildCommandChainNewObjects()
            throws Exception
    {
        Command command = new Command("foo");
        assertNotSame(command.setDirectory("foo"), command);
        assertNotSame(command.setDirectory(new File("foo")), command);
        assertNotSame(command.setSuccessfulExitCodes(42), command);
        assertNotSame(command.setSuccessfulExitCodes(ImmutableSet.of(42)), command);
        assertNotSame(command.setTimeLimit(2, TimeUnit.SECONDS), command);
        assertNotSame(command.setTimeLimit(new Duration(2, TimeUnit.SECONDS)), command);
    }

    @Test
    public void buildCommand()
            throws Exception
    {
        Command expected = new Command("a", "b", "c")
                .setDirectory("directory")
                .setSuccessfulExitCodes(33, 44)
                .setTimeLimit(5, TimeUnit.SECONDS);

        Command actual = new Command("a", "b", "c")
                .setDirectory("directory")
                .setSuccessfulExitCodes(33, 44)
                .setTimeLimit(5, TimeUnit.SECONDS);
        assertEquals(actual, expected);

        // call every setter and make sure the actual, command never changes
        assertNotSame(actual.setDirectory("foo"), actual);
        assertEquals(actual, expected);

        assertNotSame(actual.setDirectory(new File("foo")), actual);
        assertEquals(actual, expected);

        assertNotSame(actual.setSuccessfulExitCodes(42), actual);
        assertEquals(actual, expected);

        assertNotSame(actual.setSuccessfulExitCodes(ImmutableSet.of(42)), actual);
        assertEquals(actual, expected);

        assertNotSame(actual.setTimeLimit(2, TimeUnit.SECONDS), actual);
        assertEquals(actual, expected);

        assertNotSame(actual.setTimeLimit(new Duration(2, TimeUnit.SECONDS)), actual);
        assertEquals(actual, expected);
    }

    @Test
    public void testGetters()
            throws Exception
    {
        Command command = new Command("a", "b", "c")
                .setDirectory("directory")
                .setSuccessfulExitCodes(33, 44)
                .setTimeLimit(5, TimeUnit.SECONDS);

        assertEquals(command.getCommand(), ImmutableList.of("a", "b", "c"));
        assertEquals(command.getDirectory(), new File("directory"));
        assertEquals(command.getSuccessfulExitCodes(), ImmutableSet.of(44, 33));
        assertEquals(command.getTimeLimit(), new Duration(5, TimeUnit.SECONDS));
    }


    @Test
    public void execSimple()
            throws Exception
    {
        int actual = new Command("bash", "-c", "set")
                .setTimeLimit(1, TimeUnit.SECONDS)
                .execute(executor).getExitCode();
        assertEquals(actual, 0);
    }

    @SuppressWarnings("ExpectedExceptionNeverThrownTestNG")
    @Test(expectedExceptions = CommandTimeoutException.class)
    public void execTimeout()
            throws Exception
    {
        new Command("bash", "-c", "echo foo && sleep 15")
                .setTimeLimit(1, TimeUnit.SECONDS)
                .execute(executor);
    }

    @Test(expectedExceptions = CommandFailedException.class)
    public void execBadExitCode()
            throws Exception
    {
        new Command("bash", "-c", "exit 33")
                .setTimeLimit(1, TimeUnit.SECONDS)
                .execute(executor);
    }

    @Test
    public void execNonZeroSuccess()
            throws Exception
    {
        int actual = new Command("bash", "-c", "exit 33")
                .setSuccessfulExitCodes(33)
                .setTimeLimit(1, TimeUnit.SECONDS)
                .execute(executor).getExitCode();
        assertEquals(actual, 33);
    }

    @Test(expectedExceptions = CommandFailedException.class)
    public void execZeroExitFail()
            throws Exception
    {
        new Command("bash", "-c", "exit 0")
                .setSuccessfulExitCodes(33)
                .setTimeLimit(1, TimeUnit.SECONDS)
                .execute(executor);
    }

    @Test(expectedExceptions = CommandFailedException.class)
    public void execBogusProcess()
            throws Exception
    {
        new Command("ab898wer98e7r98e7r98e7r98ew")
                .setTimeLimit(1, TimeUnit.SECONDS)
                .execute(executor);
    }

    @Test
    public void testEquivalence()
    {
        equivalenceTester()
                .addEquivalentGroup(
                        new Command("command"),
                        new Command("command"))
                .addEquivalentGroup(
                        new Command("command").setDirectory("foo"),
                        new Command("command").setDirectory(new File("foo")))
                .addEquivalentGroup(
                        new Command("command").setTimeLimit(5, TimeUnit.SECONDS),
                        new Command("command").setTimeLimit(new Duration(5, TimeUnit.SECONDS)))
                .addEquivalentGroup(
                        new Command("command").setSuccessfulExitCodes(5, 6),
                        new Command("command").setSuccessfulExitCodes(ImmutableSet.of(6, 5)))
                .check();
    }
}
