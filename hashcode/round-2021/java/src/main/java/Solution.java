import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.time.StopWatch;

import lombok.AllArgsConstructor;
import lombok.Data;
import scala.Tuple2;
import scala.Tuple3;

public class Solution
{
  private static final String FILE_A = "src/main/java/a";
  private static final String FILE_B = "src/main/java/b";
  private static final String FILE_C = "src/main/java/c";
  private static final String FILE_D = "src/main/java/d";
  private static final String FILE_E = "src/main/java/e";
  private static final String FILE_F = "src/main/java/f";
  private static final String[] FILENAMES = new String[]{FILE_A, FILE_B, FILE_C, FILE_D, FILE_E, FILE_F};
  public String filename;

  @Data
  @AllArgsConstructor
  public static class Street
  {
    public int startIntersect;
    public int endIntersect;
    public String name;
    public int time;
  }

  @Data
  @AllArgsConstructor
  public static class Car
  {
    public List<String> streets;
  }

  public int duration;
  public int numOfIntersections;
  public int numOfStreets;
  public int numOfCars;
  public int bonusPoints;
  public List<Street> streets;
  public List<Car> cars;

  public Map<String, Long> streetNameToCarsNum;
  public List<List<Tuple3<Integer, String, Long>>> result;
  public List<Tuple2<String, Double>> streetNameToScores;
  public Map<String, Double> nameToOrder;

  public static void main(String[] args) throws FileNotFoundException
  {
    executeAll();
//    execute(FILE_A);
//    execute(FILE_B);
//    execute(FILE_C);
//    execute(FILE_D);
//    execute(FILE_E);
//    execute(FILE_F);
  }

  public void solve()
  {
    streetNameToCarsNum = cars.stream()
        .map(Car::getStreets)
        .flatMap(Collection::stream)
        .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
    Map<String, List<Integer>> nameToIndexList = new HashMap<>();
    cars.forEach(car -> {
      List<String> streets = car.getStreets();
      for (int i = 0; i < streets.size(); i++)
      {
        List<Integer> indexes = nameToIndexList.get(streets.get(i));
        if (CollectionUtils.isEmpty(indexes))
        {
          indexes = new ArrayList<>();
          nameToIndexList.put(streets.get(i), indexes);
        }
        indexes.add(i);
      }
    });
    nameToOrder = nameToIndexList.entrySet().stream()
        .collect(Collectors
            .toMap(Map.Entry::getKey, (entry) -> entry.getValue().stream().mapToInt(i -> i).average().getAsDouble()));
    Map<String, Integer> streetNameToTime = streets.stream()
        .collect(Collectors.toMap(Street::getName, Street::getTime));
    streetNameToScores = streetNameToCarsNum.entrySet()
        .stream()
        .map(entry -> {
          double score = (((double) entry.getValue()) / streetNameToTime.get(entry.getKey()));
          return new Tuple2<>(entry.getKey(), score);
        })
        .collect(Collectors.toList());

    Map<String, Integer> streetNameToEnd = streets.stream()
        .collect(Collectors.toMap(Street::getName, Street::getEndIntersect));
    // endIntersectId, streetName, score
    Map<Integer, List<Tuple3<Integer, String, Double>>> intersectIdToTuple3 = streetNameToScores.stream()
        .map(streetNameToScore -> new Tuple3<>(streetNameToEnd.get(streetNameToScore._1), streetNameToScore._1,
            streetNameToScore._2))
        .collect(Collectors.groupingBy(Tuple3::_1, Collectors.mapping(Function.identity(), Collectors.toList())));
    result = intersectIdToTuple3.values().stream()
        .map(this::mapWithAvgOriginal)
//        .map(this::mapWithAvg)
//        .map(this::mapProportionate)
        .filter(CollectionUtils::isNotEmpty)
        .collect(Collectors.toList());

    System.out.println();
  }

  public List<Tuple3<Integer, String, Long>> mapProportionate(List<Tuple3<Integer, String, Double>> values)
  {
    List<Tuple3<Integer, String, Long>> intersectIdToNameToScores = new ArrayList<>(values.size());
    double min = values.stream()
        .mapToDouble(Tuple3::_3)
        .min()
        .getAsDouble();
    for (int i = 0; i < values.size(); i++)
    {
      Tuple3<Integer, String, Double> intersectIdToNameToScore = values.get(i);
      long durationOnIntersect = Math.min(Math.round(intersectIdToNameToScore._3() / min), duration);
//      long durationOnIntersect = Math.round(intersectIdToNameToScore._3() / min);
      Tuple3<Integer, String, Long> tuple = new Tuple3<>(intersectIdToNameToScore._1(),
          intersectIdToNameToScore._2(), durationOnIntersect);
      intersectIdToNameToScores.add(tuple);
    }
    intersectIdToNameToScores.sort(Comparator.comparingDouble((Tuple3<Integer, String, Long> tuple3) -> nameToOrder.get(tuple3._2())).reversed());
    return intersectIdToNameToScores;
  }

  public List<Tuple3<Integer, String, Long>> mapWithAvg(List<Tuple3<Integer, String, Double>> values)
  {
    List<Tuple3<Integer, String, Long>> intersectIdToNameToScores = new ArrayList<>(values.size());
    double sum = values.stream()
        .mapToDouble(Tuple3::_3)
        .sum();
    double avg = values.stream()
        .mapToDouble(Tuple3::_3)
        .average()
        .getAsDouble();
    for (int i = 0; i < values.size(); i++)
    {
      Tuple3<Integer, String, Double> intersectIdToNameToScore = values.get(i);
      double weight = intersectIdToNameToScore._3() / sum;
      long durationOnIntersect = Math.round(avg * weight);
//      if (durationOnIntersect > 0)
//      {
        Tuple3<Integer, String, Long> tuple = new Tuple3<>(intersectIdToNameToScore._1(),
            intersectIdToNameToScore._2(), durationOnIntersect);
        intersectIdToNameToScores.add(tuple);
//      }
    }
    intersectIdToNameToScores.sort(Comparator.comparingDouble((Tuple3<Integer, String, Long> tuple3) -> nameToOrder.get(tuple3._2())).reversed());

    List<String> zeroStreets = intersectIdToNameToScores.stream()
        .filter(tuple3 -> tuple3._3() == 0)
        .map(Tuple3::_2)
        .collect(Collectors.toList());
    zeroStreets.sort(Comparator.comparingDouble(str -> nameToOrder.get(str)).reversed());


    return intersectIdToNameToScores.stream()
        .map(tuple3 -> {
          if (CollectionUtils.isNotEmpty(zeroStreets) && tuple3._2().equals(zeroStreets.get(0)))
          {
            return new Tuple3<>(tuple3._1(), tuple3._2(), 1L);
          }
          return tuple3;
        })
        .filter(tuple3 -> tuple3._3() != 0)
        .collect(Collectors.toList());
  }

  public List<Tuple3<Integer, String, Long>> mapWithAvgOriginal(List<Tuple3<Integer, String, Double>> values)
  {
    List<Tuple3<Integer, String, Long>> intersectIdToNameToScores = new ArrayList<>(values.size());
    double sum = values.stream()
        .mapToDouble(Tuple3::_3)
        .sum();
    double avg = values.stream()
        .mapToDouble(Tuple3::_3)
        .average()
        .getAsDouble();
    for (int i = 0; i < values.size(); i++)
    {
      Tuple3<Integer, String, Double> intersectIdToNameToScore = values.get(i);
      double weight = intersectIdToNameToScore._3() / sum;
//      long durationOnIntersect = Math.max(1, Math.min(Math.round(avg * weight), duration));
      long durationOnIntersect = Math.max(1, Math.round(avg * weight));
      if (durationOnIntersect > 0)
      {
        Tuple3<Integer, String, Long> tuple = new Tuple3<>(intersectIdToNameToScore._1(),
            intersectIdToNameToScore._2(), durationOnIntersect);
        intersectIdToNameToScores.add(tuple);
      }
    }
    intersectIdToNameToScores.sort(Comparator.comparingDouble((Tuple3<Integer, String, Long> tuple3) -> nameToOrder.get(tuple3._2())).reversed());

    long max = intersectIdToNameToScores.stream()
        .mapToLong(Tuple3::_3)
        .max()
        .getAsLong();
    AtomicReference<Double> percent = new AtomicReference<>();
    percent.set(1d);
    if (max > duration)
    {
//      long l = ((double) max - duration) / duration;
      percent.set(((double) duration) / max);
    }
    return intersectIdToNameToScores.stream()
        .map(tuple3 -> new Tuple3<>(tuple3._1(), tuple3._2(), Math.max(1, Math.round(tuple3._3() * percent.get()))))
        .collect(Collectors.toList());
  }

  public void input() throws FileNotFoundException
  {
    Scanner scanner = new Scanner(new FileInputStream(filename + ".txt"));
    duration = scanner.nextInt();
    numOfIntersections = scanner.nextInt();
    numOfStreets = scanner.nextInt();
    numOfCars = scanner.nextInt();
    bonusPoints = scanner.nextInt();
    streets = new ArrayList<>();
    for (int i = 0; i < numOfStreets; i++)
    {
      streets.add(new Street(scanner.nextInt(), scanner.nextInt(), scanner.next(), scanner.nextInt()));
    }
    cars = new ArrayList<>();
    for (int i = 0; i < numOfCars; i++)
    {
      int size = scanner.nextInt();
      ArrayList<String> streets = new ArrayList<>(size);
      for (int j = 0; j < size; j++)
      {
        streets.add(scanner.next());
      }
      cars.add(new Car(streets));
    }
  }

  public void output() throws FileNotFoundException
  {
    PrintWriter output = new PrintWriter(filename + ".out");
    output.print(result.size());
    result.forEach(intersectionList -> {
      output.println();
      output.println(intersectionList.get(0)._1());
      output.print(intersectionList.size());
      intersectionList.forEach(intersectIdToNameToDuration -> {
        output.println();
        output.print(intersectIdToNameToDuration._2());
        output.print(" ");
        output.print(intersectIdToNameToDuration._3());
      });
    });
    output.flush();
    output.close();
  }

  Solution(String inputFile)
  {
    filename = inputFile;
  }

  public static void executeAll() throws FileNotFoundException
  {
    for (String filename : FILENAMES)
    {
      execute(filename);
    }
  }

  public static void execute(String filename) throws FileNotFoundException
  {
    Solution solution = new Solution(filename);
    StopWatch stopWatch = StopWatch.createStarted();
    System.out.println("#############################################################################################" +
        "############################################################################################################");
    System.out.println(filename + "\t | 0 sec \t | Started execution");
    solution.input();
    System.out.println(filename + "\t | " + stopWatch.getTime(TimeUnit.SECONDS) + " sec \t | Input finished");
    stopWatch.reset();
    stopWatch.start();
    solution.solve();
    System.out.println(filename + "\t | " + stopWatch.getTime(TimeUnit.SECONDS) + " sec \t | Solve finished");
    stopWatch.reset();
    stopWatch.start();
    solution.output();
    System.out.println(filename + "\t | " + stopWatch.getTime(TimeUnit.SECONDS) + " sec \t | Output finished");
    stopWatch.stop();
    System.out.println("#############################################################################################" +
        "############################################################################################################");
  }
}
