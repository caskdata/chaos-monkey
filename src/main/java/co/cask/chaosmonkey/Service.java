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

import com.google.common.collect.ImmutableMap;

import java.io.File;

/**
 * TODO: procrastinate on documentation
 */
public class Service {

  static {
    String[] paths = {
      "hbase/hbase-hbase-regionserver.pid",
      "hbase/hbase-hbase-master.pid",
      "zookeeper/zookeeper-server.pid",
      "mysqld/mysqld.pid",
      "hive/hive-metastore.pid",
      "hadoop/yarn/yarn-yarn-resourcemanager.pid",
      "hadoop/yarn/yarn-yarn-nodemanager.pid",
      "hadoop/hdfs/hadoop-hdfs-datanode.pid",
      "hadoop/hdfs/hadoop-hdfs-namenode.pid"
    };

    Service[] services = new Service[paths.length];

    for (int i = 0; i < paths.length; i++) {
      services[i] = new Service(paths[i]);
    }

    commonServices = services;
  }

  public static final Service[] commonServices;

  public static final Service HBaseRegionServer = commonServices[0];
  public static final Service HBaseMaster = commonServices[1];
  public static final Service ZookeeperServer = commonServices[2];
  public static final Service MySQLServer = commonServices[3];
  public static final Service HiveMetastore = commonServices[4];
  public static final Service HadoopYarnResourceManager = commonServices[5];
  public static final Service HadoopYarnNodeManager = commonServices[6];
  public static final Service HadoopHdfsDataNode = commonServices[7];
  public static final Service HadoopHdfsNameNode = commonServices[8];

  public static final String baseDirectory = "/var/run/";

  private final File file;

  public Service(String path) {
    this.file = new File(baseDirectory, path);
  }

  public File getFile() {
    return this.file;
  }
}