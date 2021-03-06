/**
 * This software is licensed to you under the Apache License, Version 2.0 (the
 * "Apache License").
 *
 * LinkedIn's contributions are made under the Apache License. If you contribute
 * to the Software, the contributions will be deemed to have been made under the
 * Apache License, unless you expressly indicate otherwise. Please do not make any
 * contributions that would be inconsistent with the Apache License.
 *
 * You may obtain a copy of the Apache License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, this software
 * distributed under the Apache License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Apache
 * License for the specific language governing permissions and limitations for the
 * software governed under the Apache License.
 *
 * © 2012 LinkedIn Corp. All Rights Reserved.  
 */

package com.senseidb.gateway.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import junit.framework.TestCase;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.zookeeper.server.NIOServerCnxn;
import org.apache.zookeeper.server.ZooKeeperServer;
import org.apache.zookeeper.server.persistence.FileTxnSnapLog;
import org.json.JSONObject;
import org.junit.BeforeClass;

import proj.zoie.api.DataConsumer;
import proj.zoie.api.ZoieException;
import proj.zoie.api.DataConsumer.DataEvent;
import proj.zoie.impl.indexing.StreamDataProvider;
import proj.zoie.impl.indexing.ZoieConfig;

import com.senseidb.gateway.SenseiGateway;
import com.senseidb.plugin.SenseiPluginRegistry;

public class BaseGatewayTestUtil {

  static List<JSONObject> readDataFile() throws Exception {
    File dataFile = new File(BaseGatewayTestUtil.class.getClassLoader().getResource("test.json").toURI());
    return readData(dataFile);
  }
  
  static List<JSONObject> readData(File file) throws Exception {
    LinkedList<JSONObject> dataList = new LinkedList<JSONObject>();
    BufferedReader reader = new BufferedReader(new FileReader(file));
    while (true) {
      String line = reader.readLine();
      if (line == null)
        break;
      dataList.add(new JSONObject(line));
    }
    return dataList;
  }

  public static void compareResultList(List<JSONObject> jsonList) throws Exception{
    for (int i =0;i<jsonList.size();++i){
      String s1 = jsonList.get(i).getString("id");
      String s2 = readDataFile().get(i).getString("id");
      TestCase.assertEquals(s1, s2);
    }
  }
  
  public static void doTest(StreamDataProvider<JSONObject> dataProvider) throws Exception{
    final LinkedList<JSONObject> jsonList = new LinkedList<JSONObject>();
    
    dataProvider.setDataConsumer(new DataConsumer<JSONObject>(){

      private volatile String version;
      @Override
      public void consume(
          Collection<DataEvent<JSONObject>> events)
          throws ZoieException {

        for (DataEvent<JSONObject> event : events){
          JSONObject jsonObj = event.getData();
          System.out.println(jsonObj+", version: "+event.getVersion());
          jsonList.add(jsonObj);
          version = event.getVersion();
        }
       
      }

      @Override
      public String getVersion() {
        return version;
      }

      @Override
      public Comparator<String> getVersionComparator() {
        return ZoieConfig.DEFAULT_VERSION_COMPARATOR;
      }
      
    });
    dataProvider.start();

    int maxCount = 10;
    while(--maxCount >= 0){
      Thread.sleep(500);
      if (jsonList.size()==BaseGatewayTestUtil.readDataFile().size()){
        dataProvider.stop();
        BaseGatewayTestUtil.compareResultList(jsonList);
        break;
      }
    }
    if (maxCount < 0) {
      TestCase.fail("Timed out waiting for the gateway");
    }
  }

}
