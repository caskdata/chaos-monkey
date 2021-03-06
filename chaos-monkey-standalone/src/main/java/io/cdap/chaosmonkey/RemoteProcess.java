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

package io.cdap.chaosmonkey;

import com.jcraft.jsch.JSchException;

/**
 * A remote process that ScheduledDisruption can interact with.
 */
public interface RemoteProcess {

  /**
   * Returns this process's name.
   */
  String getName();

  /**
   * Returns the ip address of the host of this process
   */
  String getAddress();

  /**
   * Returns the path to the pid file of this process
   */
  String getPidFile();

  /**
   * Executes a command and returns the return code.
   *
   * @param command The command to be executed
   * @return The return code after running the command
   * @throws JSchException
   */
  int execAndGetReturnCode(String command) throws JSchException;

  /**
   * Executes a command and returns if it was successful.
   *
   * @param command The command to be executed
   * @return {@code true} if the command was sucessful, otherwise {@code false}
   * @throws JSchException
   */
  boolean execAndReturnSucessful(String command) throws JSchException;

  /**
   * Executes a command and return the {@link ShellOutput} of the execution
   *
   * @param command The command to be executed
   * @return The result of running the command
   * @throws JSchException
   */
  ShellOutput execAndGetOutput(String command) throws JSchException;

  /**
   * Returns whether the process is running.
   *
   * @return {@code true} if running, otherwise {@code false}.
   * @throws JSchException
   */
  boolean isRunning() throws JSchException;
}
