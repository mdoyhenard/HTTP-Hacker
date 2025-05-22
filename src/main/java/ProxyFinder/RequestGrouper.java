package ProxyFinder;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.inference.TTest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Clase para agrupar (clusterizar) y ordenar RequestSamples
 * basándose en criterios estadísticos y prácticos.
 */
public class RequestGrouper {

    // Nivel de significancia para el T-test (p-value)
    private static final double SIGNIFICANCE_LEVEL = 0.15;

    // Diferencia práctica en porcentaje (ej. 15%)
    private static final double PRACTICAL_DIFF_RATIO = 0.3;

    // Factor multiplicador del error estándar (ej. 2 => ~95% con suposiciones normales)
    private static final double STANDARD_ERROR_FACTOR = 2.5;

    /**
     * Agrupa y ordena una lista de RequestSamples:
     * 1. Cada RequestSamples empieza como un clúster independiente.
     * 2. Se fusionan los clústeres que no presentan diferencia estadísticamente significativa.
     * 3. Al final, se ordenan los clústeres resultantes de menor a mayor tiempo promedio.
     *
     * @param samples Lista de RequestSamples.
     * @return Lista de clústeres (cada clúster es un List<RequestSamples>), ordenados por tiempo promedio ascendente.
     */
    public static List<List<RequestSamples>> clusterAndSortRequestSamples(List<RequestSamples> samples) {
        // 1. Crear clústeres iniciales
        List<List<RequestSamples>> clusters = initializeClusters(samples);

        // 2. Fusión iterativa de clústeres según criterios estadísticos y prácticos
        boolean merged = true;
        while (merged && clusters.size() > 1) {
            merged = false;
            double bestPValue = -1.0;
            int bestA = -1;
            int bestB = -1;

            // Buscar el par con mayor p-value
            for (int i = 0; i < clusters.size(); i++) {
                for (int j = i + 1; j < clusters.size(); j++) {
                    double pVal = compareClusters(clusters.get(i), clusters.get(j));
                    if (pVal > bestPValue) {
                        bestPValue = pVal;
                        bestA = i;
                        bestB = j;
                    }
                }
            }

            // Si el mejor p-value supera el umbral, fusionar clústeres
            if (bestPValue > SIGNIFICANCE_LEVEL) {
                clusters.get(bestA).addAll(clusters.get(bestB));
                clusters.remove(bestB);
                merged = true;
            }
        }

        // 3. Ordenar los clústeres de menor a mayor tiempo promedio
        clusters.sort((c1, c2) -> {
            double avg1 = averageCluster(c1);
            double avg2 = averageCluster(c2);
            return Double.compare(avg1, avg2);
        });

        return clusters;
    }

    /**
     * Inicializa la lista de clústeres: cada RequestSamples en su propio clúster.
     */
    private static List<List<RequestSamples>> initializeClusters(List<RequestSamples> samples) {
        List<List<RequestSamples>> clusters = new ArrayList<>();
        for (RequestSamples rs : samples) {
            List<RequestSamples> singleCluster = new ArrayList<>();
            singleCluster.add(rs);
            clusters.add(singleCluster);
        }
        return clusters;
    }

    /**
     * Compara dos clústeres calculando un valor p "lógico" que indique
     * la posibilidad de que pertenezcan al mismo grupo.
     *
     * 1. Une los tiempos de cada clúster y filtra outliers.
     * 2. Aplica criterios de diferencia práctica y desviación estándar.
     * 3. Si no se cumplen, aplica Welch T-test para un p-value formal.
     *
     * @param clusterA Primer clúster.
     * @param clusterB Segundo clúster.
     * @return p-value que indica si no hay diferencia significativa (> 0.05 => similar).
     */
    private static double compareClusters(List<RequestSamples> clusterA, List<RequestSamples> clusterB) {
        // Recolectar todos los tiempos en 2 listas
        List<Double> dataA = gatherAllTimes(clusterA);
        List<Double> dataB = gatherAllTimes(clusterB);

        // Filtrar outliers con IQR
        dataA = filterOutliers(dataA);
        dataB = filterOutliers(dataB);

        // Si no hay suficientes datos, consideramos que son distintos
        if (dataA.size() < 2 || dataB.size() < 2) {
            return 0.0;
        }

        // Calcular estadísticos
        double[] arrA = dataA.stream().mapToDouble(Double::doubleValue).toArray();
        double[] arrB = dataB.stream().mapToDouble(Double::doubleValue).toArray();

        DescriptiveStatistics statsA = new DescriptiveStatistics(arrA);
        DescriptiveStatistics statsB = new DescriptiveStatistics(arrB);

        double meanA = statsA.getMean();
        double meanB = statsB.getMean();
        double varA = statsA.getVariance();
        double varB = statsB.getVariance();
        int nA = arrA.length;
        int nB = arrB.length;

        // 1) Criterio de diferencia práctica (porcentaje)
        double diff = Math.abs(meanA - meanB);
        double practicalThreshold = Math.min(meanA, meanB) * PRACTICAL_DIFF_RATIO;
        if (diff <= practicalThreshold) {
            // En la práctica, son casi iguales
            return 1.0; // forzamos la fusión
        }

        // 2) Criterio basado en el error estándar de la diferencia
        double se = Math.sqrt((varA / nA) + (varB / nB));
        if (diff <= STANDARD_ERROR_FACTOR * se) {
            // La diferencia está dentro de X SE => no es significativa en la práctica
            return 1.0;
        }

        // 3) Welch T-test formal (Commons Math)
        TTest ttest = new TTest();
        double pValue;
        try {
            pValue = ttest.tTest(arrA, arrB);
        } catch (IllegalArgumentException e) {
            pValue = 0.0;
        }
        return pValue;
    }

    public static boolean areSameGroup(List<RequestSamples> clusterA,
                                       List<RequestSamples> clusterB) {
        double pVal = compareClusters(clusterA, clusterB);
        // Si pVal es mayor que tu umbral (SIGNIFICANCE_LEVEL), indicaría
        // que NO se puede rechazar la hipótesis de igualdad estadística
        // (es decir, probablemente son "el mismo grupo")
        return pVal > SIGNIFICANCE_LEVEL;
    }

    /**
     * Recolecta todos los tiempos de un clúster (lista de RequestSamples) en una sola lista.
     */
    private static List<Double> gatherAllTimes(List<RequestSamples> cluster) {
        List<Double> times = new ArrayList<>();
        for (RequestSamples rs : cluster) {
            for (Long val : rs.getTimeSamples()) {
                times.add(val.doubleValue());
            }
        }
        return times;
    }

    /**
     * Filtra outliers de una lista de doubles usando el rango intercuartílico (IQR).
     */
    private static List<Double> filterOutliers(List<Double> data) {
        if (data.size() < 2) {
            return data;
        }
        List<Double> sorted = new ArrayList<>(data);
        Collections.sort(sorted);

        double q1 = percentile(sorted, 25);
        double q3 = percentile(sorted, 75);
        double iqr = q3 - q1;

        double lower = q1 - 1.5 * iqr;
        double upper = q3 + 1.5 * iqr;

        List<Double> filtered = new ArrayList<>();
        for (double d : sorted) {
            if (d >= lower && d <= upper) {
                filtered.add(d);
            }
        }
        return filtered;
    }

    /**
     * Calcula un percentil p (0-100) de una lista ordenada.
     */
    private static double percentile(List<Double> sorted, double p) {
        if (sorted.isEmpty()) {
            return 0.0;
        }
        if (sorted.size() == 1) {
            return sorted.get(0);
        }

        double rank = (p / 100.0) * (sorted.size() - 1);
        int lowerIndex = (int) Math.floor(rank);
        int upperIndex = (int) Math.ceil(rank);

        if (lowerIndex == upperIndex) {
            return sorted.get(lowerIndex);
        }

        double weight = rank - lowerIndex;
        return sorted.get(lowerIndex) * (1 - weight) + sorted.get(upperIndex) * weight;
    }

    /**
     * Calcula el promedio de un clúster de RequestSamples.
     *
     * @param cluster Lista de RequestSamples
     * @return El tiempo promedio del clúster.
     */
    private static double averageCluster(List<RequestSamples> cluster) {
        List<Double> allTimes = gatherAllTimes(cluster);
        if (allTimes.isEmpty()) {
            return 0.0;
        }
        double sum = 0.0;
        for (double t : allTimes) {
            sum += t;
        }
        return sum / allTimes.size();
    }
}
