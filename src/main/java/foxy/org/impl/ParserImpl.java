package foxy.org.impl;

import foxy.org.Parser;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

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
        itemIds.add("STONE");
        itemIds.add("STONEBLOCK");


        Map<String, Map<String, String>> data = new HashMap<>();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless"); // Запуск браузера в "безголовом" режиме (без отображения GUI)
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");

        // Создание ExecutorService для многопоточности
        ExecutorService executor = Executors.newFixedThreadPool(4);

        List<Future<Map.Entry<String, Map<String, String>>>> futures = new ArrayList<>();

        for (int tear = 2; tear <= 5; tear++) {
            for (String s : itemIds) {
                String url = productLink + tear + "_" + s;
                futures.add(executor.submit(() -> fetchData(url, options)));
            }
        }

        // Получение результатов
        for (Future<Map.Entry<String, Map<String, String>>> future : futures) {
            try {
                Map.Entry<String, Map<String, String>> result = future.get();
                if (result != null) {
                    data.put(result.getKey(), result.getValue());
                }
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        // Завершение работы ExecutorService
        executor.shutdown();
    }

    private Map.Entry<String, Map<String, String>> fetchData(String url, ChromeOptions options) {
        WebDriver driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS); // Неявное ожидание

        try {
            driver.get(url);

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
            WebElement tableElement = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("table.table.table-striped")));

            // Парсинг заголовка h1
            WebElement headerElement = driver.findElement(By.tagName("h1"));
            String header = headerElement.getText();

            // Поиск таблицы
            WebElement table = driver.findElement(By.cssSelector("table.table.table-striped tbody#market-table-body"));
            if (table != null) {
                Map<String, String> tableData = new HashMap<>();

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

                return new AbstractMap.SimpleEntry<>(header, tableData);
            } else {
                System.out.println("Таблица не найдена на странице: " + url);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            driver.quit();
        }
        return null;
    }


}
