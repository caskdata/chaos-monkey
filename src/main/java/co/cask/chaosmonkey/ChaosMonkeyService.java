/*
 * Copyright © 2017 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package co.cask.chaosmonkey;

import co.cask.chaosmonkey.conf.Configuration;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.jcraft.jsch.JSchException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

/**
 * The main service that will be running ChaosMonkey.
 */
public class ChaosMonkeyService extends AbstractScheduledService {
  private static final Logger LOGGER = LoggerFactory.getLogger(ChaosMonkeyService.class);

  private RemoteProcess process;
  private double stopProbability;
  private double killProbability;
  private double restartProbability;
  private int executionPeriod;

  /**
   *
   * @param process The processes that will be managed
   * @param stopProbability Probability that this process will be stopped in the current interval
   * @param killProbability Probability that this process will be killed in the current interval
   * @param restartProbability Probability that this process will be restarted in the current interval
   * @param executionPeriod The rate of execution cycles (in seconds)
   */
  public ChaosMonkeyService(RemoteProcess process,
                            double stopProbability,
                            double killProbability,
                            double restartProbability,
                            int executionPeriod) {
    this.process = process;
    this.stopProbability = stopProbability;
    this.killProbability = killProbability;
    this.restartProbability = restartProbability;
    this.executionPeriod = executionPeriod;
  }

  @Override
  protected void runOneIteration() throws Exception {
    double random = Math.random();

    boolean serviceRunningBeforeIteration = process.isRunning();
    if (random < stopProbability && serviceRunningBeforeIteration) {
      LOGGER.info("Attempting to stop {}", process.getName());
      process.stop();
    } else if (random < stopProbability + killProbability && serviceRunningBeforeIteration) {
      LOGGER.info("Attempting to kill {}", process.getName());
      process.kill();
    } else if (random < stopProbability + killProbability + restartProbability && !serviceRunningBeforeIteration) {
      LOGGER.info("Attempting to restart {}", process.getName());
      process.restart();
    } else {
      return;
    }

    // Only do a check after an action has been attempted
    boolean serviceRunningAfterIteration = process.isRunning();
    if (serviceRunningBeforeIteration && serviceRunningAfterIteration) {
      LOGGER.error("{} is still running!", process.getName());
    } else if (serviceRunningBeforeIteration && !serviceRunningAfterIteration) {
      LOGGER.info("{} is no longer running", process.getName());
    } else if (!serviceRunningBeforeIteration && serviceRunningAfterIteration) {
      LOGGER.info("{} is now running", process.getName());
    } else if (!serviceRunningBeforeIteration && !serviceRunningAfterIteration) {
      LOGGER.error("{} did not restart", process.getName());
    }

  }

  @Override
  protected Scheduler scheduler() {
    return AbstractScheduledService.Scheduler.newFixedRateSchedule(0, this.executionPeriod, TimeUnit.SECONDS);
  }

  /**
   * The main method for this class.
   *
   * @param args
   * @throws JSchException if a SSH-related error occurs
   * @throws IllegalArgumentException if an invalid configuration file is given
   * @throws IOException if there was an error getting cluster information from Coopr
   */
  public static void main(String[] args) throws JSchException, IllegalArgumentException, IOException {
    Configuration conf = Configuration.create();

    String username = conf.get("username", System.getProperty("user.name"));
    String privateKey = conf.get("privateKey");
    String keyPassphrase = conf.get("keyPassphrase");

    Collection<ChaosMonkeyService> services = new LinkedList<>();
    Collection<NodeProperties> propertiesList = ChaosMonkeyHelper.getNodeProperties(conf).values();

    for (NodeProperties nodeProperties : propertiesList) {
      SshShell sshShell;
      if (privateKey != null) {
        if (keyPassphrase != null) {
          sshShell = new SshShell(username, nodeProperties.getAccessIpAddress(), privateKey);
        } else {
          sshShell = new SshShell(username, nodeProperties.getAccessIpAddress(), privateKey, keyPassphrase);
        }
      } else {
        sshShell = new SshShell(username, nodeProperties.getAccessIpAddress());
      }

      for (String service : nodeProperties.getServices()) {
        String pidPath = conf.get(service + ".pidPath");
        if (pidPath == null) {
          throw new IllegalArgumentException("The following process does not have a pidPath: " + service);
        }

        int interval;
        try {
          interval = conf.getInt(service + ".interval");
        } catch (NumberFormatException | NullPointerException e) {
          throw new IllegalArgumentException("The following process does not have a valid interval: " + service, e);
        }

        double killProbability = conf.getDouble(service + ".killProbability", 0.0);
        double stopProbability = conf.getDouble(service + ".stopProbability", 0.0);
        double restartProbability = conf.getDouble(service + ".restartProbability", 0.0);

        if (killProbability == 0.0 && stopProbability == 0.0 && restartProbability == 0.0) {
          throw new IllegalArgumentException("The following process may not have all of killProbability, " +
                                               "stopProbability and restartProbability equal to 0.0 or undefined: "
                                               + service);
        }
        if (stopProbability + killProbability + restartProbability > 1) {
          throw new IllegalArgumentException("The following process has a combined killProbability, stopProbability" +
                                               " and restartProbability of over 1.0: " + service);
        }

        String statusCommand = conf.get(service + ".statusCommand");

        RemoteProcess process;
        if (statusCommand != null) {
          process = new RemoteProcess(service, pidPath, sshShell, statusCommand);
        } else {
          process = new RemoteProcess(service, pidPath, sshShell);
        }
        if (process.exists()) {
          LOGGER.debug("Created {} with pidPath: {}, stopProbability: {}, killProbability: {}, " +
                         "restartProbability: {}, interval: {}",
                       service, pidPath, stopProbability, killProbability, restartProbability, interval);
          ChaosMonkeyService chaosMonkeyService = new ChaosMonkeyService(process, stopProbability, killProbability,
                                                                         restartProbability, interval);

          LOGGER.debug("The {} service has been added for {}@{}",
                       service, sshShell.getUsername(), nodeProperties.getAccessIpAddress());
          services.add(chaosMonkeyService);
        } else {
          LOGGER.info("The {} service does not exist on {}@{}... Skipping",
                      service, sshShell.getUsername(), nodeProperties.getAccessIpAddress());
        }
      }
    }

    for (ChaosMonkeyService service : services) {
      service.startAsync();
    }
  }
}
