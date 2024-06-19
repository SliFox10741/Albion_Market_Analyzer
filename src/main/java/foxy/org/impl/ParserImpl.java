package foxy.org.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import foxy.org.Parser;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.net.SocketException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ParserImpl implements Parser {
    String productLink = "https://europe.albiononline2d.com/en/item/id/T";

    @Override
    public void parsSite() {
        List<String> itemIds = new ArrayList<>();
        itemIds.add("WOOD");
        itemIds.add("CLOTH");
        itemIds.add("FIBER");
        itemIds.add("HIDE");
        itemIds.add("LEATHER");
        itemIds.add("METALBAR");
        itemIds.add("ORE");
        itemIds.add("PLANKS");
        itemIds.add("ROCK");
        itemIds.add("STONEBLOCK");

        Map<String, Map<String, String>> data = new ConcurrentHashMap<>();

        // Настройка ChromeOptions для отключения изображений и ускорения загрузки страницы
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless"); // Запуск браузера в "безголовом" режиме (без отображения GUI)
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");

        // Создание ExecutorService для многопоточности
        ExecutorService executor = Executors.newFixedThreadPool(2);

        for (int tear = 2; tear <= 3; tear++) {
            for (String s : itemIds) {
                String url = productLink + tear + "_" + s;
                executor.submit(() -> fetchData(url, options, data));
            }
        }

        // Завершение работы ExecutorService
        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Сохранение данных в JSON-файл
        saveDataToJson(data, "data.json");
        ObjectMapper mapper = new ObjectMapper();

        try {
            Map<String, Map<String, String>> value = mapper.readValue(new File("data.json"), Map.class);

            System.out.println(value.get("Birch Logs (T2)").get("Lymhurst:"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void fetchData(String url, ChromeOptions options, Map<String, Map<String, String>> data) {
        WebDriver driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5)); // Неявное ожидание

        try {
            driver.get(url);

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            WebElement tableElement = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("table.table.table-striped")));

            // Парсинг заголовка h1
            WebElement headerElement = driver.findElement(By.tagName("h1"));
            String header = headerElement.getText();

            // Поиск таблицы
            WebElement table = driver.findElement(By.cssSelector("table.table.table-striped tbody#market-table-body"));
            if (table != null) {
                Map<String, String> tableData = new ConcurrentHashMap<>();

                // Разбиваем таблицу на строки
                List<WebElement> rows = table.findElements(By.tagName("tr"));
                for (WebElement row : rows) {
                    List<WebElement> columns = row.findElements(By.tagName("td"));
                    if (columns.size() >= 2) {
                        String value1 = columns.get(0).getText();
                        String value2 = columns.get(1).getText();
                        tableData.put(value1, value2);
                    }
                }

                data.put(header, tableData);
            } else {
                System.out.println("Таблица не найдена на странице: " + url);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            driver.quit();
        }
    }

    private void saveDataToJson(Map<String, Map<String, String>> data, String filePath) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        try {
            mapper.writeValue(new File(filePath), data);
            System.out.println("Данные успешно сохранены в файл " + filePath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}