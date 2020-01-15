package com.okay;

import org.apache.jmeter.engine.StandardJMeterEngine;
import org.apache.jmeter.save.SaveService;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.collections.HashTree;

import java.io.File;


/**
 * @author zhou
 * @date 2020/1/6
 */
public class JMeterFromExistingJMX {

    public static void main(String[] argv) throws Exception {
        // JMeter Engine
        StandardJMeterEngine jmeter = new StandardJMeterEngine();


        // Initialize Properties, logging, locale, etc.
        JMeterUtils.loadJMeterProperties("D:\\apache-jmeter-5.1.1\\bin\\jmeter.properties");
        JMeterUtils.setJMeterHome("D:\\apache-jmeter-5.1.1");
        JMeterUtils.initLogging();// you can comment this line out to see extra log messages of i.e. DEBUG level
        JMeterUtils.initLocale();

        // Initialize JMeter SaveService
        SaveService.loadProperties();

        // Load existing .jmx Test Plan
        File in = new File("D:\\apache-jmeter-5.1.1\\bin\\Java压测.jmx");
        HashTree testPlanTree = SaveService.loadTree(in);
        //增加结果收集
        SummariserUtils summer = null;
        String summariserName = JMeterUtils.getPropDefault("summariser.name", "summary");
        if (summariserName.length() > 0) {
            summer = new SummariserUtils(summariserName);
        }
        ResultCollectorUtil logger = new ResultCollectorUtil(summer);
        testPlanTree.add(testPlanTree.getArray(), logger);
        // Run JMeter Test
        jmeter.configure(testPlanTree);
        jmeter.run();
    }
}
