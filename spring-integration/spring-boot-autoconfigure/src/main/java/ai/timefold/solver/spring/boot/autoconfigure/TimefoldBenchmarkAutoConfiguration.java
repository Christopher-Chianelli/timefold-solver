package ai.timefold.solver.spring.boot.autoconfigure;

import static java.util.Objects.requireNonNullElse;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import ai.timefold.solver.benchmark.api.PlannerBenchmarkFactory;
import ai.timefold.solver.benchmark.config.PlannerBenchmarkConfig;
import ai.timefold.solver.benchmark.config.SolverBenchmarkConfig;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.spring.boot.autoconfigure.config.BenchmarkProperties;
import ai.timefold.solver.spring.boot.autoconfigure.config.TimefoldProperties;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@AutoConfigureAfter(TimefoldAutoConfiguration.class)
@ConditionalOnClass({ PlannerBenchmarkFactory.class })
@ConditionalOnMissingBean({ PlannerBenchmarkFactory.class })
@EnableConfigurationProperties({ TimefoldProperties.class })
public class TimefoldBenchmarkAutoConfiguration
        implements BeanClassLoaderAware {

    private final TimefoldProperties timefoldProperties;
    private ClassLoader beanClassLoader;

    protected TimefoldBenchmarkAutoConfiguration(TimefoldProperties timefoldProperties) {
        this.timefoldProperties = timefoldProperties;
    }

    @Bean
    @ConditionalOnBean({ SolverConfig.class })
    public PlannerBenchmarkConfig plannerBenchmarkConfig(SolverConfig solverConfig) {
        if (solverConfig == null) {
            return null;
        }
        PlannerBenchmarkConfig benchmarkConfig;
        if (timefoldProperties.getBenchmark() != null
                && timefoldProperties.getBenchmark().getSolverBenchmarkConfigXml() != null) {
            if (beanClassLoader.getResource(timefoldProperties.getBenchmark().getSolverBenchmarkConfigXml()) == null) {
                throw new IllegalStateException(
                        "Invalid timefold.benchmark.solverBenchmarkConfigXml property ("
                                + timefoldProperties.getBenchmark().getSolverBenchmarkConfigXml()
                                + "): that classpath resource does not exist.");
            }
            benchmarkConfig = PlannerBenchmarkConfig
                    .createFromXmlResource(timefoldProperties.getBenchmark().getSolverBenchmarkConfigXml(), beanClassLoader);
        } else if (beanClassLoader.getResource(BenchmarkProperties.DEFAULT_SOLVER_BENCHMARK_CONFIG_URL) != null) {
            benchmarkConfig = PlannerBenchmarkConfig.createFromXmlResource(
                    TimefoldProperties.DEFAULT_SOLVER_BENCHMARK_CONFIG_URL, beanClassLoader);
        } else {
            benchmarkConfig = PlannerBenchmarkConfig.createFromSolverConfig(solverConfig);
            benchmarkConfig.setBenchmarkDirectory(new File(BenchmarkProperties.DEFAULT_BENCHMARK_RESULT_DIRECTORY));
        }

        if (timefoldProperties.getBenchmark() != null && timefoldProperties.getBenchmark().getResultDirectory() != null) {
            benchmarkConfig.setBenchmarkDirectory(new File(timefoldProperties.getBenchmark().getResultDirectory()));
        }

        if (benchmarkConfig.getBenchmarkDirectory() == null) {
            benchmarkConfig.setBenchmarkDirectory(new File(BenchmarkProperties.DEFAULT_BENCHMARK_RESULT_DIRECTORY));
        }

        if (timefoldProperties.getBenchmark() != null && timefoldProperties.getBenchmark().getSolver() != null) {
            TimefoldAutoConfiguration
                    .applyTerminationProperties(benchmarkConfig.getInheritedSolverBenchmarkConfig().getSolverConfig(),
                            timefoldProperties.getBenchmark().getSolver().getTermination());
        }

        if (benchmarkConfig.getInheritedSolverBenchmarkConfig().getSolverConfig().getTerminationConfig() == null ||
                !benchmarkConfig.getInheritedSolverBenchmarkConfig().getSolverConfig().getTerminationConfig().isConfigured()) {
            List<SolverBenchmarkConfig> solverBenchmarkConfigList = benchmarkConfig.getSolverBenchmarkConfigList();
            List<String> unconfiguredTerminationSolverBenchmarkList = new ArrayList<>();
            if (solverBenchmarkConfigList == null) {
                throw new IllegalStateException("At least one of the properties " +
                        "timefold.benchmark.solver.termination.spent-limit, " +
                        "timefold.benchmark.solver.termination.best-score-limit, " +
                        "timefold.benchmark.solver.termination.unimproved-spent-limit " +
                        "is required if termination is not configured in the " +
                        "inherited solver benchmark config and solverBenchmarkBluePrint is used.");
            }
            if (solverBenchmarkConfigList.size() == 1 && solverBenchmarkConfigList.get(0).getSolverConfig() == null) {
                // Benchmark config was created from solver config, which means only the inherited solver config exists.
                SolverBenchmarkConfig solverBenchmarkConfig = benchmarkConfig.getInheritedSolverBenchmarkConfig();
                if (!solverBenchmarkConfig.getSolverConfig().canTerminate()) {
                    String benchmarkConfigName =
                            requireNonNullElse(solverBenchmarkConfig.getName(), "InheritedSolverBenchmarkConfig");
                    unconfiguredTerminationSolverBenchmarkList.add(benchmarkConfigName);
                }
            } else {
                for (int i = 0; i < solverBenchmarkConfigList.size(); i++) {
                    SolverBenchmarkConfig solverBenchmarkConfig = solverBenchmarkConfigList.get(i);
                    if (!solverConfig.canTerminate()) {
                        String benchmarkConfigName =
                                requireNonNullElse(solverBenchmarkConfig.getName(), "SolverBenchmarkConfig" + i);
                        unconfiguredTerminationSolverBenchmarkList.add(benchmarkConfigName);
                    }
                }
            }
            if (!unconfiguredTerminationSolverBenchmarkList.isEmpty()) {
                throw new IllegalStateException("The following " + SolverBenchmarkConfig.class.getSimpleName() + " do not " +
                        "have termination configured: " +
                        unconfiguredTerminationSolverBenchmarkList.stream()
                                .collect(Collectors.joining(", ", "[", "]"))
                        + ". " +
                        "At least one of the properties " +
                        "timefold.benchmark.solver.termination.spent-limit, " +
                        "timefold.benchmark.solver.termination.best-score-limit, " +
                        "timefold.benchmark.solver.termination.unimproved-spent-limit " +
                        "is required if termination is not configured in a solver benchmark and the " +
                        "inherited solver benchmark config.");
            }
        }
        return benchmarkConfig;
    }

    @Bean
    @ConditionalOnBean({ PlannerBenchmarkConfig.class })
    public PlannerBenchmarkFactory plannerBenchmarkFactory(PlannerBenchmarkConfig benchmarkConfig) {
        if (benchmarkConfig == null) {
            return null;
        }
        return PlannerBenchmarkFactory.create(benchmarkConfig);
    }

    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        this.beanClassLoader = classLoader;
    }
}
