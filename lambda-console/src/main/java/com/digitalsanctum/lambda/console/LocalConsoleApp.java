package com.digitalsanctum.lambda.console;

import com.digitalsanctum.lambda.Executor;
import com.digitalsanctum.lambda.generator.Generator;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;
import javax.ws.rs.GET;
import javax.ws.rs.POST;

public class LocalConsoleApp {

    private final String lambdaJar;
    private final String lambdaHandler;
    private final String lambdaResourcePath;
    private final Class httpMethodClazz;

    public LocalConsoleApp(String lambdaJar, String lambdaHandler, String lambdaResourcePath, String httpMethod) {
        this.lambdaJar = lambdaJar;
        this.lambdaHandler = lambdaHandler;
        this.lambdaResourcePath = lambdaResourcePath;
        this.httpMethodClazz = Objects.equals(httpMethod, "POST") ? POST.class : GET.class;
    }

    public static void main(String[] args) throws Exception {
        long start = System.currentTimeMillis();

        String lambdaJar = args[0];
        String lambdaHandler = args[1];
        String lambdaResourcePath = args[2];
        String httpMethod = args[3];

        // dynamically adds lambda jar to classloader
        addLambdaJar(new File(lambdaJar));

        // introspects lambdaHandler and generates a Jersey resource/app
        new LocalConsoleApp(lambdaJar, lambdaHandler, lambdaResourcePath, httpMethod)
                .build();

        System.out.println("time = " + (System.currentTimeMillis() - start));
    }

    private void build() throws Exception {

        String apiImplModule = "lambda-api-gateway-jersey";

        String baseDir = "/Users/shane.witbeck/projects/lambda";
        String apiDir = baseDir + "/" + apiImplModule;
        Path endpointSrcPath = Paths.get(apiDir, "/src/main/java");
        Path pomFilePath = Paths.get(apiDir, "/pom.xml");

        Path srcApiJar = Paths.get(apiDir, "/target/" + apiImplModule + "-1.0-SNAPSHOT.jar");
        Path exportedApiJar = Paths.get(baseDir, "/export/api.jar");

        Map<String, Class> types = Executor.getRequestHandlerTypes(lambdaHandler);
        Class requestType = types.get("request");

        new Generator()
                .installLambdaJar(pomFilePath, Paths.get(lambdaJar))
                .generateJerseyResource(endpointSrcPath, httpMethodClazz, lambdaResourcePath, requestType)
                .compileAndPackageGateway(pomFilePath)
                .exportGatewayJar(srcApiJar, exportedApiJar);
    }

    private static void addLambdaJar(File file) throws Exception {
        Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
        method.setAccessible(true);
        method.invoke(ClassLoader.getSystemClassLoader(), file.toURI().toURL());
    }
}