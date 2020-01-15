package com.okay;


import org.apache.jmeter.JMeter;
import org.apache.jmeter.assertions.ResponseAssertion;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.config.gui.ArgumentsPanel;
import org.apache.jmeter.control.LoopController;
import org.apache.jmeter.control.gui.LoopControlPanel;
import org.apache.jmeter.control.gui.TestPlanGui;
import org.apache.jmeter.engine.StandardJMeterEngine;
import org.apache.jmeter.protocol.http.control.Cookie;
import org.apache.jmeter.protocol.http.control.CookieManager;
import org.apache.jmeter.protocol.http.control.Header;
import org.apache.jmeter.protocol.http.control.HeaderManager;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;
import org.apache.jmeter.protocol.http.util.HTTPArgument;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.TestPlan;
import org.apache.jmeter.threads.ThreadGroup;
import org.apache.jmeter.threads.gui.ThreadGroupGui;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.collections.HashTree;
import org.joor.Reflect;

import java.io.File;

/**
 * @author zhou
 * @date 2019/12/30
 */
public class TestPlanLauncher {

    private static final String checkData = "\"cityid\":\"101010100\"";


    public static void main(String[] args) {
        // jemter 引擎
        StandardJMeterEngine standardJMeterEngine = new StandardJMeterEngine();
        // 设置不适用gui的方式调用jmeter
        System.setProperty(JMeter.JMETER_NON_GUI, "true");
        // 设置jmeter.properties文件，我们将jmeter文件存放在resources中，通过classload
        String path = TestPlanLauncher.class.getClassLoader().getResource("jmeter.properties").getPath();
        File jmeterPropertiesFile = new File(path);
        if (jmeterPropertiesFile.exists()) {
            JMeterUtils.loadJMeterProperties(jmeterPropertiesFile.getPath());
            HashTree testPlanTree = new HashTree();
            // 创建测试计划
            TestPlan testPlan = createTestPlan();
            // 创建线程组
            ThreadGroup threadGroup = createThreadGroup();
            // 创建循环控制器
            LoopController loopController = createLoopController();
            // 线程组设置循环控制
            threadGroup.setSamplerController(loopController);
            // 创建http请求收集器
            HTTPSamplerProxy examplecomSampler = createHTTPSamplerProxy();
            //创建检查点
            ResponseAssertion responseAssertion = createResponseAssertion(checkData);
            //创建cookie
            CookieManager cookieManager = createCookieManager();
            //创建header
            HeaderManager headerManager = createHeaderManager();
            // 将测试计划添加到测试配置树种
            HashTree threadGroupHashTree = testPlanTree.add(testPlan, threadGroup);
            // 将http请求采样器添加到线程组下
            threadGroupHashTree.add(examplecomSampler);
            threadGroupHashTree.add(responseAssertion);
            threadGroupHashTree.add(cookieManager);
            threadGroupHashTree.add(headerManager);
            //增加结果收集
            SummariserUtils summer = null;
            String summariserName = JMeterUtils.getPropDefault("summariser.name", "summary");
            if (summariserName.length() > 0) {
                summer = new SummariserUtils(summariserName);
            }
            ResultCollectorUtil logger = new ResultCollectorUtil(summer);
            testPlanTree.add(testPlanTree.getArray(), logger);

            // 配置jmeter
            standardJMeterEngine.configure(testPlanTree);
            // 运行
            standardJMeterEngine.run();


            //获取聚合报告结果
            String info = Reflect.on(summer).get("summerMessage");


            float throughtPut = Float.parseFloat(info.split("=")[2].split("/")[0].trim());
            int responseTime = Integer.parseInt(info.split("Avg:")[1].split("Min")[0].trim());
            String errorRate = info.split("\\(")[1].split("%")[0].trim();
            System.out.println("throughtPut:" + throughtPut);
            System.out.println("responseTime:" + responseTime);
            System.out.println("errorRate:" + errorRate);
            System.out.println("info:" + info);
            String fail = info.split("Err:")[1].split("\\(")[0].trim();
            int failCount = Integer.parseInt(fail);
            if (failCount > 0) {
                System.out.println("fail==" + failCount);
            } else {
                System.out.println("pass==" + failCount);
            }


        }
    }

    /**
     * 创建测试计划
     * <p>
     * return TestPlan
     */
    public static TestPlan createTestPlan() {

        //创建测试计划
        TestPlan testPlan = new TestPlan("压测");
        testPlan.setProperty(TestElement.TEST_CLASS, TestPlan.class.getName());
        testPlan.setProperty(TestElement.GUI_CLASS, TestPlanGui.class.getName());
        testPlan.setUserDefinedVariables((Arguments) new ArgumentsPanel().createTestElement());
        testPlan.setSerialized(true);
        testPlan.setTearDownOnShutdown(true);
        return testPlan;
    }


    /**
     * 创建线程组
     *
     * @return
     */
    public static ThreadGroup createThreadGroup() {

        // Thread Group
        ThreadGroup threadGroup = new ThreadGroup();
        threadGroup.setName("线程组");
        threadGroup.setNumThreads(2);
        threadGroup.setRampUp(0);
        threadGroup.setScheduler(true);
        threadGroup.setDuration(1);
        threadGroup.setDelay(0);
        threadGroup.setSamplerController(createLoopController());
        threadGroup.setProperty(TestElement.TEST_CLASS, ThreadGroup.class.getName());
        threadGroup.setProperty(TestElement.GUI_CLASS, ThreadGroupGui.class.getName());
        return threadGroup;
    }

    /**
     * 创建循环控制器
     *
     * @return
     */
    public static LoopController createLoopController() {
        // Loop Controller
        LoopController loopController = new LoopController();
        loopController.setLoops(-1);
        loopController.setFirst(true);
        loopController.setProperty(TestElement.TEST_CLASS, LoopController.class.getName());
        loopController.setProperty(TestElement.GUI_CLASS, LoopControlPanel.class.getName());
        loopController.initialize();
        return loopController;
    }

    /**
     * 创建http采样器
     *
     * @return
     */
    public static HTTPSamplerProxy createHTTPSamplerProxy() {
//        HTTPSamplerProxy httpSamplerProxy = new HTTPSamplerProxy();
//        httpSamplerProxy.setName("天气预报");
//        httpSamplerProxy.setDomain("www.weather.com.cn");
//        httpSamplerProxy.setPort(80);
//        httpSamplerProxy.setPath("/data/sk/101010100.html");
//        httpSamplerProxy.setMethod("GET");
//        httpSamplerProxy.setConnectTimeout("5000");
//        httpSamplerProxy.setUseKeepAlive(true);
//        httpSamplerProxy.setHeaderManager(headerManager);
//        httpSamplerProxy.setProperty(TestElement.TEST_CLASS, HTTPSamplerProxy.class.getName());
//        httpSamplerProxy.setProperty(TestElement.GUI_CLASS, HttpTestSampleGui.class.getName());


        HeaderManager headerManager = new HeaderManager();
        headerManager.setProperty("requestId", "jmeter_${__time(yyyyMMddHHmmssSSS)}${__Random(10000,99999,)}");
        String data = "{\"os\":\"X510\",\"sw\":\"1920\",\"class_id\":\"\",\"channel\":\"t_pad\",\"type_publish\":\"1\",\"contype\":3,\"ua\":\"OKAY_EBOOK_4.0.0_TCOKUI_4.3.0.46_20190121_T\",\"vc\":628,\"mac\":\"02:00:00:00:00:00\",\"token\":\"26344a5132464448a5986cec28d2bcf4\",\"screen_pattern\":\"1\",\"uid\":\"61951058256\",\"iccid\":\"\",\"sh\":\"1200\",\"serial\":\"d389f052\",\"imei\":\"0\",\"page\":\"1\",\"directory_id\":\"0\",\"udid\":\"6F20C82BC060F67AE339D9AB98F4503C\",\"vs\":\"2.8.5.9_S4-debug\"}";
//        String body = data.replace("\"","&quot;");
        HTTPSamplerProxy httpSamplerProxy = new HTTPSamplerProxy();
        httpSamplerProxy.setName("已发布课程");
        httpSamplerProxy.setPort(80);
        httpSamplerProxy.setMethod("POST");
        httpSamplerProxy.setPath("https://teacherpad-dev.xk12.cn/api/t_pad/res_package/pkg_list_pub");
        httpSamplerProxy.setConnectTimeout("5000");
        httpSamplerProxy.setAutoRedirects(false);
        httpSamplerProxy.setUseKeepAlive(true);
        httpSamplerProxy.setFollowRedirects(true);
        httpSamplerProxy.setPostBodyRaw(true);
        httpSamplerProxy.setHeaderManager(headerManager);

        //添加body参数
        HTTPArgument httpArgument = new HTTPArgument();
        httpArgument.setValue(data);
        httpArgument.setAlwaysEncoded(false);
        httpArgument.setMetaData("=");
        Arguments arguments = new Arguments();
        arguments.addArgument(httpArgument);
        httpSamplerProxy.setArguments(arguments);
        return httpSamplerProxy;
    }


    /**
     * 设置检查点
     * return
     */

    private static ResponseAssertion createResponseAssertion(String checkData) {

        //设置检查点
        ResponseAssertion responseAssertion = new ResponseAssertion();
        responseAssertion.addTestString(checkData);
        responseAssertion.setTestFieldResponseData();
        return responseAssertion;
    }

    private static CookieManager createCookieManager() {

        CookieManager cookieManager = new CookieManager();


        Cookie cookie = new Cookie();

        cookie.setName("test");
        cookie.setValue("ddd");
        cookie.setDomain("www.weather.com.cn");
        cookie.setPath("/");
        cookie.setSecure(true);
        cookie.setVersion(0);
        cookieManager.add(cookie);
        return cookieManager;
    }

    private static HeaderManager createHeaderManager() {

        HeaderManager headerManager = new HeaderManager();

        Header header = new Header();
        header.setName("requestId");
        header.setValue("jmeter-0123456");
        headerManager.add(header);

        return headerManager;

    }

}
