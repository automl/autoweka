_smac_validate()
{
	#Adapted from http://www.debian-administration.org/article/317/An_introduction_to_bash_completion_part_2
	local cur prev opts base
	COMPREPLY=()
	cur="${COMP_WORDS[COMP_CWORD]}"
	prev="${COMP_WORDS[COMP_CWORD-1]}"
	case "${prev}" in
	--configuration-list|--help-default-file|--validation-defaults-file|--scenario-file|--scenario|--search-subspace-file|--run-hashcode-file|--tae-default-file)
		 _filedir
		return 0
		;;
	--prepost-exec-dir)
		answers=" readable directories "
		COMPREPLY=( $(compgen -W "${answers}" -- ${cur}) )
 		return 0
		;;
	--tae|--fork-to-tae)
		answers="ANALYTIC  BLACKHOLE  CLI  CONSTANT  IPC  PRELOADED  RANDOM"
		COMPREPLY=( $(compgen -W "${answers}" -- ${cur}) )
 		return 0
		;;
	--help-level)
		answers="BASIC  INTERMEDIATE  ADVANCED  DEVELOPER"
		COMPREPLY=( $(compgen -W "${answers}" -- ${cur}) )
 		return 0
		;;
	--fork-to-tae-policy)
		answers="DUPLICATE_ON_SLAVE  DUPLICATE_ON_SLAVE_QUICK"
		COMPREPLY=( $(compgen -W "${answers}" -- ${cur}) )
 		return 0
		;;
	--inter-obj|--inter-instance-obj|--inter_instance_obj|--intra-obj|--intra-instance-obj|--overall-obj|--overall_obj|--intra_instance_obj)
		answers="MEAN  MEAN1000  MEAN10"
		COMPREPLY=( $(compgen -W "${answers}" -- ${cur}) )
 		return 0
		;;
	--run-obj|--run-objective|--run_obj)
		answers="RUNTIME  QUALITY"
		COMPREPLY=( $(compgen -W "${answers}" -- ${cur}) )
 		return 0
		;;
	--console-log-level|--log-level)
		answers="TRACE  DEBUG  INFO  WARN  ERROR  OFF"
		COMPREPLY=( $(compgen -W "${answers}" -- ${cur}) )
 		return 0
		;;
	--validation-rounding-mode)
		answers="UP  NONE"
		COMPREPLY=( $(compgen -W "${answers}" -- ${cur}) )
 		return 0
		;;
	--auto-increment-tunertime|--includeDefaultAsFirstRandom|--trajectory-use-tunertime-if-no-walltime|--use-scenario-outdir|--validate-test-instances|--wait-for-persistent-run-completion|--check-instances-exist|--skip-features|--ignore-features|--use-instances|--use-cpu-time-in-tunertime|--algo-deterministic|--deterministic|--abort-on-crash|--abort-on-first-run-crash|--bound-runs|--cache-runs|--cache-runs-debug|--cache-runs-strictly-increasing-observer|--call-observer-before-completion|--check-for-unclean-shutdown|--check-for-unique-runconfigs|--check-for-unique-runconfigs-exception|--check-result-order-consistent|--check-sat-consistency|--checkSATConsistency|--check-sat-consistency-exception|--exception-on-prepost-command|--exit-on-failure|--file-cache|--file-cache-crash-on-cache-miss|--file-cache-crash-on-miss|--filter-zero-cutoff-runs|--kill-run-exceeding-captime|--leak-memory|--log-requests-responses|--log-requests-responses-rc-only|--log-requests-responses-rc|--observer-walltime-if-no-runtime|--prepost-log-output|--skip-outstanding-eval-tae|--synchronize-observers|--tae-stop-processing-on-shutdown|--track-scheduled-runs|--transform-crashed-quality|--use-dynamic-cutoffs|--verify-sat|--verify-SAT|--verifySAT|--tae-transform|--tae-transform-valid-values-only|--save-state-file|--validate-all|--validate-by-wallclock-time|--validate-only-last-incumbent|--validation-headers)
		answers="true  false"
		COMPREPLY=( $(compgen -W "${answers}" -- ${cur}) )
 		return 0
		;;
	*)
		answers="--abort-on-crash --abort-on-first-run-crash --algo --algo-cutoff-length --algo-cutoff-time --algo-deterministic --algo-exec --algo-exec-dir --auto-increment-tunertime --bound-runs --cache-runs --cache-runs-debug --cache-runs-strictly-increasing-observer --call-observer-before-completion --check-for-unclean-shutdown --check-for-unique-runconfigs --check-for-unique-runconfigs-exception --check-instances-exist --check-result-order-consistent --check-sat-consistency --check-sat-consistency-exception --checkSATConsistency --configuration --configuration-list --console-log-level --continous-neighbours --continuous-neighbors --cores --cputime-limit --cputime_limit --cutoff-time --cutoff_length --cutoff_time --deterministic --empirical-performance --exception-on-prepost-command --exec-dir --execdir --exit-on-failure --experiment-dir --feature-file --feature_file --file-cache --file-cache-crash-on-cache-miss --file-cache-crash-on-miss --file-cache-output --file-cache-source --filter-zero-cutoff-runs --fork-to-tae --fork-to-tae-duplicate-on-slave-quick-timeout --fork-to-tae-policy --help --help-default-file --help-level --ignore-features --includeDefaultAsFirstRandom --instance-dir --instance-file --instance-regex --instance-suffix --instance_file --instance_seed_file --instances --inter-instance-obj --inter-obj --inter_instance_obj --intra-instance-obj --intra-obj --intra_instance_obj --invalid-scenario-reason --iteration-limit --kill-run-exceeding-captime --kill-run-exceeding-captime-factor --kill-runs-on-file-delete --leak-memory --leak-memory-amount --log-level --log-requests-responses --log-requests-responses-rc --log-requests-responses-rc-only --max-norun-challenge-limit --max-timestamp --min-timestamp --mult-factor --num-run --num-seeds-per-test-instance --num-test-instances --num-validation-runs --numrun --observer-walltime-delay --observer-walltime-if-no-runtime --observer-walltime-scale --outdir --output-dir --output-file-suffix --overall-obj --overall_obj --param-file --paramfile --pcs-file --post-scenario-command --post_cmd --pre-scenario-command --pre_cmd --prepost-exec-dir --prepost-log-output --random --random-configurations --retry-crashed-count --run-hashcode-file --run-obj --run-objective --run_obj --runcount-limit --runcount_limit --runtime-limit --save-state-file --scenario --scenario-file --search-subspace --search-subspace-file --seed --seed-offset --show-hidden --skip-features --skip-outstanding-eval-tae --synchronize-observers --tae --tae-default-file --tae-stop-processing-on-shutdown --tae-transform --tae-transform-SAT-quality --tae-transform-SAT-runtime --tae-transform-TIMEOUT-quality --tae-transform-TIMEOUT-runtime --tae-transform-UNSAT-quality --tae-transform-UNSAT-runtime --tae-transform-other-quality --tae-transform-other-runtime --tae-transform-valid-values-only --tae-warn-if-no-response-from-tae --target-run-cputime-limit --target_run_cputime_limit --terminate-on-delete --test-instance-dir --test-instance-file --test-instance-regex --test-instance-suffix --test-instances --test_instance_file --test_instance_seed_file --track-scheduled-runs --track-scheduled-runs-resolution --trajectory-file --trajectory-files --trajectory-use-tunertime-if-no-walltime --transform-crashed-quality --transform-crashed-quality-value --tuner-overhead-time --tuner-timeout --tunertime-limit --use-cpu-time-in-tunertime --use-dynamic-cutoffs --use-instances --use-scenario-outdir --validate-all --validate-by-wallclock-time --validate-only-if-tunertime-reached --validate-only-if-walltime-reached --validate-only-last-incumbent --validate-test-instances --validation-defaults-file --validation-headers --validation-rounding-mode --validation-tunertime --verify-SAT --verify-sat --verifySAT --version --wait-for-persistent-run-completion --wall-time --wallclock-limit --wallclock_limit "
		if [[ ${cur} == -* ]] ; then
        		COMPREPLY=( $(compgen -W "${answers}" -- ${cur}) )
	        	return 0
    		fi
		;;
	esac
}
complete -F _smac_validate smac-validate

_smac()
{
	#Adapted from http://www.debian-administration.org/article/317/An_introduction_to_bash_completion_part_2
	local cur prev opts base
	COMPREPLY=()
	cur="${COMP_WORDS[COMP_CWORD]}"
	prev="${COMP_WORDS[COMP_CWORD-1]}"
	case "${prev}" in
	--help-default-file|--model-hashcode-file|--option-file|--option-file2|--restore-scenario|--smac-default-file|--scenario-file|--scenario|--search-subspace-file|--run-hashcode-file|--tae-default-file)
		 _filedir
		return 0
		;;
	--prepost-exec-dir)
		answers=" readable directories "
		COMPREPLY=( $(compgen -W "${answers}" -- ${cur}) )
 		return 0
		;;
	--tae|--fork-to-tae)
		answers="ANALYTIC  BLACKHOLE  CLI  CONSTANT  IPC  PRELOADED  RANDOM"
		COMPREPLY=( $(compgen -W "${answers}" -- ${cur}) )
 		return 0
		;;
	--help-level)
		answers="BASIC  INTERMEDIATE  ADVANCED  DEVELOPER"
		COMPREPLY=( $(compgen -W "${answers}" -- ${cur}) )
 		return 0
		;;
	--init-mode|--initialization-mode)
		answers="CLASSIC  ITERATIVE_CAPPING  UNBIASED_TABLE"
		COMPREPLY=( $(compgen -W "${answers}" -- ${cur}) )
 		return 0
		;;
	--fork-to-tae-policy)
		answers="DUPLICATE_ON_SLAVE  DUPLICATE_ON_SLAVE_QUICK"
		COMPREPLY=( $(compgen -W "${answers}" -- ${cur}) )
 		return 0
		;;
	--acq-func|--acquisition-function|--ei-func|--expected-improvement-function)
		answers="EXPONENTIAL  SIMPLE  LCB  EI  LCBEIRR"
		COMPREPLY=( $(compgen -W "${answers}" -- ${cur}) )
 		return 0
		;;
	--inter-obj|--inter-instance-obj|--inter_instance_obj|--intra-obj|--intra-instance-obj|--overall-obj|--overall_obj|--intra_instance_obj)
		answers="MEAN  MEAN1000  MEAN10"
		COMPREPLY=( $(compgen -W "${answers}" -- ${cur}) )
 		return 0
		;;
	--state-deserializer|--state-serializer)
		answers="NULL  LEGACY"
		COMPREPLY=( $(compgen -W "${answers}" -- ${cur}) )
 		return 0
		;;
	--run-obj|--run-objective|--run_obj)
		answers="RUNTIME  QUALITY"
		COMPREPLY=( $(compgen -W "${answers}" -- ${cur}) )
 		return 0
		;;
	--exec-mode|--execution-mode)
		answers="SMAC  ROAR  PSEL"
		COMPREPLY=( $(compgen -W "${answers}" -- ${cur}) )
 		return 0
		;;
	--console-log-level|--log-level)
		answers="TRACE  DEBUG  INFO  WARN  ERROR  OFF"
		COMPREPLY=( $(compgen -W "${answers}" -- ${cur}) )
 		return 0
		;;
	--validation-rounding-mode)
		answers="UP  NONE"
		COMPREPLY=( $(compgen -W "${answers}" -- ${cur}) )
 		return 0
		;;
	--shared-model-mode-default-handling)
		answers="USE_ALL  SKIP_FIRST_TWO  IGNORE_ALL"
		COMPREPLY=( $(compgen -W "${answers}" -- ${cur}) )
 		return 0
		;;
	--adaptive-capping|--ac|--always-run-initial-config|--clean-old-state-on-success|--config-tracking|--deterministic-instance-ordering|--intermediary-saves|--iterativeCappingBreakOnFirstCompletion|--mask-censored-data-as-kappa-max|--mask-inactive-conditional-parameters-as-default-value|--print-rungroup-replacement-and-exit|--quick-saves|--save-context|--save-runs-every-iteration|--shared-model-mode|--share-model-mode|--shared-run-data|--share-run-data|--shared-model-mode-asymetric|--shared-model-mode-tae|--shared-model-mode-write-data|--write-json-data|--treat-censored-data-as-uncensored|--validation|--rf-full-tree-bootstrap|--rf-ignore-conditionality|--rf-impute-mean|--rf-log-model|--log-model|--rf-penalize-imputed-values|--rf-preprocess-marginal|preprocessMarginal|--rf-shuffle-imputed-values|--rf-store-data|--rf-store-data-in-leaves|--rf-subsample-values-when-low-on-memory|--useBrokenVarianceCalculation|--check-instances-exist|--skip-features|--ignore-features|--use-instances|--use-cpu-time-in-tunertime|--algo-deterministic|--deterministic|--abort-on-crash|--abort-on-first-run-crash|--bound-runs|--cache-runs|--cache-runs-debug|--cache-runs-strictly-increasing-observer|--call-observer-before-completion|--check-for-unclean-shutdown|--check-for-unique-runconfigs|--check-for-unique-runconfigs-exception|--check-result-order-consistent|--check-sat-consistency|--checkSATConsistency|--check-sat-consistency-exception|--exception-on-prepost-command|--exit-on-failure|--file-cache|--file-cache-crash-on-cache-miss|--file-cache-crash-on-miss|--filter-zero-cutoff-runs|--kill-run-exceeding-captime|--leak-memory|--log-requests-responses|--log-requests-responses-rc-only|--log-requests-responses-rc|--observer-walltime-if-no-runtime|--prepost-log-output|--skip-outstanding-eval-tae|--synchronize-observers|--tae-stop-processing-on-shutdown|--track-scheduled-runs|--transform-crashed-quality|--use-dynamic-cutoffs|--verify-sat|--verify-SAT|--verifySAT|--tae-transform|--tae-transform-valid-values-only|--save-state-file|--validate-all|--validate-by-wallclock-time|--validate-only-last-incumbent|--validation-headers)
		answers="true  false"
		COMPREPLY=( $(compgen -W "${answers}" -- ${cur}) )
 		return 0
		;;
	*)
		answers="--abort-on-crash --abort-on-first-run-crash --ac --ac-add-slack --ac-mult-slack --acq-func --acquisition-function --adaptive-capping --algo --algo-cutoff-length --algo-cutoff-time --algo-deterministic --algo-exec --algo-exec-dir --always-run-initial-config --bound-runs --cache-runs --cache-runs-debug --cache-runs-strictly-increasing-observer --call-observer-before-completion --check-for-unclean-shutdown --check-for-unique-runconfigs --check-for-unique-runconfigs-exception --check-instances-exist --check-result-order-consistent --check-sat-consistency --check-sat-consistency-exception --checkSATConsistency --clean-old-state-on-success --config-tracking --console-log-level --continous-neighbours --continuous-neighbors --cores --cputime-limit --cputime_limit --cutoff-time --cutoff_length --cutoff_time --deterministic --deterministic-instance-ordering --doubling-capping-challengers --doubling-capping-runs-per-challenger --ei-func --exception-on-prepost-command --exec-dir --exec-mode --execdir --execution-mode --exit-on-failure --expected-improvement-function --experiment-dir --feature-file --feature_file --file-cache --file-cache-crash-on-cache-miss --file-cache-crash-on-miss --file-cache-output --file-cache-source --filter-zero-cutoff-runs --fork-to-tae --fork-to-tae-duplicate-on-slave-quick-timeout --fork-to-tae-policy --frac_rawruntime --help --help-default-file --help-level --ignore-features --imputation-iterations --init-mode --initial-challenger-runs --initial-challengers --initial-challengers-intensification-time --initial-incumbent --initial-incumbent-runs --initialN --initialization-mode --instance-dir --instance-file --instance-regex --instance-suffix --instance_file --instance_seed_file --instances --intensification-percentage --inter-instance-obj --inter-obj --inter_instance_obj --intermediary-saves --intra-instance-obj --intra-obj --intra_instance_obj --invalid-scenario-reason --iteration-limit --iterativeCappingBreakOnFirstCompletion --iterativeCappingK --kill-run-exceeding-captime --kill-run-exceeding-captime-factor --kill-runs-on-file-delete --leak-memory --leak-memory-amount --log-level --log-model --log-requests-responses --log-requests-responses-rc --log-requests-responses-rc-only --mask-censored-data-as-kappa-max --mask-inactive-conditional-parameters-as-default-value --max-incumbent-runs --max-norun-challenge-limit --max-timestamp --min-timestamp --model-hashcode-file --mult-factor --num-challengers --num-ei-random --num-local-search-random --num-ls-random --num-pca --num-run --num-seeds-per-test-instance --num-test-instances --num-trees --num-validation-runs --numPCA --numrun --observer-walltime-delay --observer-walltime-if-no-runtime --observer-walltime-scale --option-file --option-file2 --outdir --output-dir --output-file-suffix --overall-obj --overall_obj --param-file --paramfile --pcs-file --post-scenario-command --post_cmd --pre-scenario-command --pre_cmd --prepost-exec-dir --prepost-log-output --print-rungroup-replacement-and-exit --quick-saves --restore-iteration --restore-scenario --restore-state-from --retry-crashed-count --rf-full-tree-bootstrap --rf-ignore-conditionality --rf-impute-mean --rf-log-model --rf-min-variance --rf-num-trees --rf-penalize-imputed-values --rf-preprocess-marginal --rf-ratio-features --rf-shuffle-imputed-values --rf-split-min --rf-store-data --rf-store-data-in-leaves --rf-subsample-memory-percentage --rf-subsample-percentage --rf-subsample-values-when-low-on-memory --run-hashcode-file --run-obj --run-objective --run_obj --runcount-limit --runcount_limit --rungroup --rungroup-char --rungroup-name --runtime-limit --save-context --save-runs-every-iteration --save-state-file --scenario --scenario-file --search-subspace --search-subspace-file --seed --seed-offset --share-model-mode --share-model-mode-frequency --share-run-data --share-run-data-frequency --shared-model-mode --shared-model-mode-asymetric --shared-model-mode-default-handling --shared-model-mode-frequency --shared-model-mode-tae --shared-model-mode-write-data --shared-run-data --shared-run-data-frequency --show-hidden --skip-features --skip-outstanding-eval-tae --smac-default-file --split-min --state-deserializer --state-serializer --synchronize-observers --tae --tae-default-file --tae-stop-processing-on-shutdown --tae-transform --tae-transform-SAT-quality --tae-transform-SAT-runtime --tae-transform-TIMEOUT-quality --tae-transform-TIMEOUT-runtime --tae-transform-UNSAT-quality --tae-transform-UNSAT-runtime --tae-transform-other-quality --tae-transform-other-runtime --tae-transform-valid-values-only --tae-warn-if-no-response-from-tae --target-run-cputime-limit --target_run_cputime_limit --terminate-on-delete --test-instance-dir --test-instance-file --test-instance-regex --test-instance-suffix --test-instances --test_instance_file --test_instance_seed_file --track-scheduled-runs --track-scheduled-runs-resolution --transform-crashed-quality --transform-crashed-quality-value --treat-censored-data-as-uncensored --tuner-timeout --tunertime-limit --unbiased-capping-challengers --unbiased-capping-cpulimit --unbiased-capping-runs-per-challenger --use-cpu-time-in-tunertime --use-dynamic-cutoffs --use-instances --useBrokenVarianceCalculation --validate-all --validate-by-wallclock-time --validate-only-if-tunertime-reached --validate-only-if-walltime-reached --validate-only-last-incumbent --validation --validation-cores --validation-headers --validation-rounding-mode --validation-seed --verify-SAT --verify-sat --verifySAT --version --wallclock-limit --wallclock_limit --warmstart --warmstart-from --warmstart-iteration --write-json-data "
		if [[ ${cur} == -* ]] ; then
        		COMPREPLY=( $(compgen -W "${answers}" -- ${cur}) )
	        	return 0
    		fi
		;;
	esac
}
complete -F _smac smac

_algo_test()
{
	#Adapted from http://www.debian-administration.org/article/317/An_introduction_to_bash_completion_part_2
	local cur prev opts base
	COMPREPLY=()
	cur="${COMP_WORDS[COMP_CWORD]}"
	prev="${COMP_WORDS[COMP_CWORD-1]}"
	case "${prev}" in
	--help-default-file|--taeRunnerDefaultsFile|--scenario-file|--scenario|--search-subspace-file|--run-hashcode-file|--tae-default-file)
		 _filedir
		return 0
		;;
	--prepost-exec-dir)
		answers=" readable directories "
		COMPREPLY=( $(compgen -W "${answers}" -- ${cur}) )
 		return 0
		;;
	--tae|--fork-to-tae)
		answers="ANALYTIC  BLACKHOLE  CLI  CONSTANT  IPC  PRELOADED  RANDOM"
		COMPREPLY=( $(compgen -W "${answers}" -- ${cur}) )
 		return 0
		;;
	--help-level)
		answers="BASIC  INTERMEDIATE  ADVANCED  DEVELOPER"
		COMPREPLY=( $(compgen -W "${answers}" -- ${cur}) )
 		return 0
		;;
	--fork-to-tae-policy)
		answers="DUPLICATE_ON_SLAVE  DUPLICATE_ON_SLAVE_QUICK"
		COMPREPLY=( $(compgen -W "${answers}" -- ${cur}) )
 		return 0
		;;
	--instance-selection)
		answers="FIRST  RANDOM"
		COMPREPLY=( $(compgen -W "${answers}" -- ${cur}) )
 		return 0
		;;
	--inter-obj|--inter-instance-obj|--inter_instance_obj|--intra-obj|--intra-instance-obj|--overall-obj|--overall_obj|--intra_instance_obj)
		answers="MEAN  MEAN1000  MEAN10"
		COMPREPLY=( $(compgen -W "${answers}" -- ${cur}) )
 		return 0
		;;
	--run-obj|--run-objective|--run_obj)
		answers="RUNTIME  QUALITY"
		COMPREPLY=( $(compgen -W "${answers}" -- ${cur}) )
 		return 0
		;;
	--log-level|--console-log-level)
		answers="TRACE  DEBUG  INFO  WARN  ERROR  OFF"
		COMPREPLY=( $(compgen -W "${answers}" -- ${cur}) )
 		return 0
		;;
	--print-json|--check-instances-exist|--skip-features|--ignore-features|--use-instances|--use-cpu-time-in-tunertime|--algo-deterministic|--deterministic|--abort-on-crash|--abort-on-first-run-crash|--bound-runs|--cache-runs|--cache-runs-debug|--cache-runs-strictly-increasing-observer|--call-observer-before-completion|--check-for-unclean-shutdown|--check-for-unique-runconfigs|--check-for-unique-runconfigs-exception|--check-result-order-consistent|--check-sat-consistency|--checkSATConsistency|--check-sat-consistency-exception|--exception-on-prepost-command|--exit-on-failure|--file-cache|--file-cache-crash-on-cache-miss|--file-cache-crash-on-miss|--filter-zero-cutoff-runs|--kill-run-exceeding-captime|--leak-memory|--log-requests-responses|--log-requests-responses-rc-only|--log-requests-responses-rc|--observer-walltime-if-no-runtime|--prepost-log-output|--skip-outstanding-eval-tae|--synchronize-observers|--tae-stop-processing-on-shutdown|--track-scheduled-runs|--transform-crashed-quality|--use-dynamic-cutoffs|--verify-sat|--verify-SAT|--verifySAT|--tae-transform|--tae-transform-valid-values-only)
		answers="true  false"
		COMPREPLY=( $(compgen -W "${answers}" -- ${cur}) )
 		return 0
		;;
	*)
		answers="--abort-on-crash --abort-on-first-run-crash --algo --algo-cutoff-length --algo-cutoff-time --algo-deterministic --algo-exec --algo-exec-dir --bound-runs --cache-runs --cache-runs-debug --cache-runs-strictly-increasing-observer --call-observer-before-completion --check-for-unclean-shutdown --check-for-unique-runconfigs --check-for-unique-runconfigs-exception --check-instances-exist --check-result-order-consistent --check-sat-consistency --check-sat-consistency-exception --checkSATConsistency --config --config-seed --console-log-level --continous-neighbours --continuous-neighbors --cores --cputime-limit --cputime_limit --cutoff-time --cutoff_length --cutoff_time --deterministic --exception-on-prepost-command --exec-dir --execdir --exit-on-failure --feature-file --feature_file --file-cache --file-cache-crash-on-cache-miss --file-cache-crash-on-miss --file-cache-output --file-cache-source --filter-zero-cutoff-runs --fork-to-tae --fork-to-tae-duplicate-on-slave-quick-timeout --fork-to-tae-policy --help --help-default-file --help-level --ignore-features --instance --instance-dir --instance-file --instance-regex --instance-selection --instance-suffix --instance_file --instance_seed_file --instances --inter-instance-obj --inter-obj --inter_instance_obj --intra-instance-obj --intra-obj --intra_instance_obj --invalid-scenario-reason --iteration-limit --kill-run-exceeding-captime --kill-run-exceeding-captime-factor --kill-runs-on-file-delete --kill-time --leak-memory --leak-memory-amount --log-level --log-requests-responses --log-requests-responses-rc --log-requests-responses-rc-only --max-norun-challenge-limit --observer-walltime-delay --observer-walltime-if-no-runtime --observer-walltime-scale --outdir --output-dir --overall-obj --overall_obj --param-file --paramfile --pcs-file --post-scenario-command --post_cmd --pre-scenario-command --pre_cmd --prepost-exec-dir --prepost-log-output --print-json --retry-crashed-count --run-hashcode-file --run-obj --run-objective --run_obj --runcount-limit --runcount_limit --runtime-limit --scenario --scenario-file --search-subspace --search-subspace-file --seed --show-hidden --skip-features --skip-outstanding-eval-tae --synchronize-observers --tae --tae-default-file --tae-stop-processing-on-shutdown --tae-transform --tae-transform-SAT-quality --tae-transform-SAT-runtime --tae-transform-TIMEOUT-quality --tae-transform-TIMEOUT-runtime --tae-transform-UNSAT-quality --tae-transform-UNSAT-runtime --tae-transform-other-quality --tae-transform-other-runtime --tae-transform-valid-values-only --tae-warn-if-no-response-from-tae --taeRunnerDefaultsFile --target-run-cputime-limit --target_run_cputime_limit --terminate-on-delete --test-instance-dir --test-instance-file --test-instance-regex --test-instance-suffix --test-instances --test_instance_file --test_instance_seed_file --track-scheduled-runs --track-scheduled-runs-resolution --transform-crashed-quality --transform-crashed-quality-value --tuner-timeout --tunertime-limit --use-cpu-time-in-tunertime --use-dynamic-cutoffs --use-instances --verify-SAT --verify-sat --verifySAT --version --wallclock-limit --wallclock_limit "
		if [[ ${cur} == -* ]] ; then
        		COMPREPLY=( $(compgen -W "${answers}" -- ${cur}) )
	        	return 0
    		fi
		;;
	esac
}
complete -F _algo_test algo-test

_sat_check()
{
	#Adapted from http://www.debian-administration.org/article/317/An_introduction_to_bash_completion_part_2
	local cur prev opts base
	COMPREPLY=()
	cur="${COMP_WORDS[COMP_CWORD]}"
	prev="${COMP_WORDS[COMP_CWORD-1]}"
	case "${prev}" in
	--help-default-file|--taeRunnerDefaultsFile|--scenario-file|--scenario|--search-subspace-file|--run-hashcode-file|--tae-default-file)
		 _filedir
		return 0
		;;
	--prepost-exec-dir)
		answers=" readable directories "
		COMPREPLY=( $(compgen -W "${answers}" -- ${cur}) )
 		return 0
		;;
	--tae|--fork-to-tae)
		answers="ANALYTIC  BLACKHOLE  CLI  CONSTANT  IPC  PRELOADED  RANDOM"
		COMPREPLY=( $(compgen -W "${answers}" -- ${cur}) )
 		return 0
		;;
	--help-level)
		answers="BASIC  INTERMEDIATE  ADVANCED  DEVELOPER"
		COMPREPLY=( $(compgen -W "${answers}" -- ${cur}) )
 		return 0
		;;
	--fork-to-tae-policy)
		answers="DUPLICATE_ON_SLAVE  DUPLICATE_ON_SLAVE_QUICK"
		COMPREPLY=( $(compgen -W "${answers}" -- ${cur}) )
 		return 0
		;;
	--inter-obj|--inter-instance-obj|--inter_instance_obj|--intra-obj|--intra-instance-obj|--overall-obj|--overall_obj|--intra_instance_obj)
		answers="MEAN  MEAN1000  MEAN10"
		COMPREPLY=( $(compgen -W "${answers}" -- ${cur}) )
 		return 0
		;;
	--run-obj|--run-objective|--run_obj)
		answers="RUNTIME  QUALITY"
		COMPREPLY=( $(compgen -W "${answers}" -- ${cur}) )
 		return 0
		;;
	--log-level|--console-log-level)
		answers="TRACE  DEBUG  INFO  WARN  ERROR  OFF"
		COMPREPLY=( $(compgen -W "${answers}" -- ${cur}) )
 		return 0
		;;
	--overwrite-output-file|--use-test-set-instances|--check-instances-exist|--skip-features|--ignore-features|--use-instances|--use-cpu-time-in-tunertime|--algo-deterministic|--deterministic|--abort-on-crash|--abort-on-first-run-crash|--bound-runs|--cache-runs|--cache-runs-debug|--cache-runs-strictly-increasing-observer|--call-observer-before-completion|--check-for-unclean-shutdown|--check-for-unique-runconfigs|--check-for-unique-runconfigs-exception|--check-result-order-consistent|--check-sat-consistency|--checkSATConsistency|--check-sat-consistency-exception|--exception-on-prepost-command|--exit-on-failure|--file-cache|--file-cache-crash-on-cache-miss|--file-cache-crash-on-miss|--filter-zero-cutoff-runs|--kill-run-exceeding-captime|--leak-memory|--log-requests-responses|--log-requests-responses-rc-only|--log-requests-responses-rc|--observer-walltime-if-no-runtime|--prepost-log-output|--skip-outstanding-eval-tae|--synchronize-observers|--tae-stop-processing-on-shutdown|--track-scheduled-runs|--transform-crashed-quality|--use-dynamic-cutoffs|--verify-sat|--verify-SAT|--verifySAT|--tae-transform|--tae-transform-valid-values-only)
		answers="true  false"
		COMPREPLY=( $(compgen -W "${answers}" -- ${cur}) )
 		return 0
		;;
	*)
		answers="--abort-on-crash --abort-on-first-run-crash --algo --algo-cutoff-length --algo-cutoff-time --algo-deterministic --algo-exec --algo-exec-dir --bound-runs --cache-runs --cache-runs-debug --cache-runs-strictly-increasing-observer --call-observer-before-completion --check-for-unclean-shutdown --check-for-unique-runconfigs --check-for-unique-runconfigs-exception --check-instances-exist --check-result-order-consistent --check-sat-consistency --check-sat-consistency-exception --checkSATConsistency --config --config-seed --console-log-level --continous-neighbours --continuous-neighbors --cores --cputime-limit --cputime_limit --cutoff-time --cutoff_length --cutoff_time --deterministic --exception-on-prepost-command --exec-dir --execdir --exit-on-failure --feature-file --feature_file --file-cache --file-cache-crash-on-cache-miss --file-cache-crash-on-miss --file-cache-output --file-cache-source --filter-zero-cutoff-runs --fork-to-tae --fork-to-tae-duplicate-on-slave-quick-timeout --fork-to-tae-policy --help --help-default-file --help-level --ignore-features --instance-dir --instance-file --instance-regex --instance-suffix --instance_file --instance_seed_file --instances --inter-instance-obj --inter-obj --inter_instance_obj --intra-instance-obj --intra-obj --intra_instance_obj --invalid-scenario-reason --iteration-limit --kill-run-exceeding-captime --kill-run-exceeding-captime-factor --kill-runs-on-file-delete --leak-memory --leak-memory-amount --log-level --log-requests-responses --log-requests-responses-rc --log-requests-responses-rc-only --max-norun-challenge-limit --observer-walltime-delay --observer-walltime-if-no-runtime --observer-walltime-scale --outdir --output-dir --output-file --overall-obj --overall_obj --overwrite-output-file --param-file --paramfile --pcs-file --post-scenario-command --post_cmd --pre-scenario-command --pre_cmd --prepost-exec-dir --prepost-log-output --retry-crashed-count --run-hashcode-file --run-obj --run-objective --run_obj --runcount-limit --runcount_limit --runtime-limit --scenario --scenario-file --search-subspace --search-subspace-file --seed --show-hidden --skip-features --skip-outstanding-eval-tae --synchronize-observers --tae --tae-default-file --tae-stop-processing-on-shutdown --tae-transform --tae-transform-SAT-quality --tae-transform-SAT-runtime --tae-transform-TIMEOUT-quality --tae-transform-TIMEOUT-runtime --tae-transform-UNSAT-quality --tae-transform-UNSAT-runtime --tae-transform-other-quality --tae-transform-other-runtime --tae-transform-valid-values-only --tae-warn-if-no-response-from-tae --taeRunnerDefaultsFile --target-run-cputime-limit --target_run_cputime_limit --terminate-on-delete --test-instance-dir --test-instance-file --test-instance-regex --test-instance-suffix --test-instances --test_instance_file --test_instance_seed_file --track-scheduled-runs --track-scheduled-runs-resolution --transform-crashed-quality --transform-crashed-quality-value --tuner-timeout --tunertime-limit --use-cpu-time-in-tunertime --use-dynamic-cutoffs --use-instances --use-test-set-instances --verify-SAT --verify-sat --verifySAT --version --wallclock-limit --wallclock_limit "
		if [[ ${cur} == -* ]] ; then
        		COMPREPLY=( $(compgen -W "${answers}" -- ${cur}) )
	        	return 0
    		fi
		;;
	esac
}
complete -F _sat_check sat-check

_verify_scenario()
{
	#Adapted from http://www.debian-administration.org/article/317/An_introduction_to_bash_completion_part_2
	local cur prev opts base
	COMPREPLY=()
	cur="${COMP_WORDS[COMP_CWORD]}"
	prev="${COMP_WORDS[COMP_CWORD-1]}"
	case "${prev}" in
	--help-default-file)
		 _filedir
		return 0
		;;
	--help-level)
		answers="BASIC  INTERMEDIATE  ADVANCED  DEVELOPER"
		COMPREPLY=( $(compgen -W "${answers}" -- ${cur}) )
 		return 0
		;;
	--log-level|--console-log-level)
		answers="TRACE  DEBUG  INFO  WARN  ERROR  OFF"
		COMPREPLY=( $(compgen -W "${answers}" -- ${cur}) )
 		return 0
		;;
	--output-details|--verify-instances)
		answers="true  false"
		COMPREPLY=( $(compgen -W "${answers}" -- ${cur}) )
 		return 0
		;;
	*)
		answers="--console-log-level --experiment-dir --help --help-default-file --help-level --log-level --output-details --restore-args --restore-scenario-arguments --scenario --scenario-file --scenario-files --scenarios --show-hidden --verify-instances --version "
		if [[ ${cur} == -* ]] ; then
        		COMPREPLY=( $(compgen -W "${answers}" -- ${cur}) )
	        	return 0
    		fi
		;;
	esac
}
complete -F _verify_scenario verify-scenario

_state_merge()
{
	#Adapted from http://www.debian-administration.org/article/317/An_introduction_to_bash_completion_part_2
	local cur prev opts base
	COMPREPLY=()
	cur="${COMP_WORDS[COMP_CWORD]}"
	prev="${COMP_WORDS[COMP_CWORD-1]}"
	case "${prev}" in
	--help-default-file|--scenario-file|--scenario|--search-subspace-file|--run-hashcode-file|--tae-default-file)
		 _filedir
		return 0
		;;
	--prepost-exec-dir)
		answers=" readable directories "
		COMPREPLY=( $(compgen -W "${answers}" -- ${cur}) )
 		return 0
		;;
	--tae|--fork-to-tae)
		answers="ANALYTIC  BLACKHOLE  CLI  CONSTANT  IPC  PRELOADED  RANDOM"
		COMPREPLY=( $(compgen -W "${answers}" -- ${cur}) )
 		return 0
		;;
	--help-level)
		answers="BASIC  INTERMEDIATE  ADVANCED  DEVELOPER"
		COMPREPLY=( $(compgen -W "${answers}" -- ${cur}) )
 		return 0
		;;
	--fork-to-tae-policy)
		answers="DUPLICATE_ON_SLAVE  DUPLICATE_ON_SLAVE_QUICK"
		COMPREPLY=( $(compgen -W "${answers}" -- ${cur}) )
 		return 0
		;;
	--inter-obj|--inter-instance-obj|--inter_instance_obj|--intra-obj|--intra-instance-obj|--overall-obj|--overall_obj|--intra_instance_obj)
		answers="MEAN  MEAN1000  MEAN10"
		COMPREPLY=( $(compgen -W "${answers}" -- ${cur}) )
 		return 0
		;;
	--run-obj|--run-objective|--run_obj)
		answers="RUNTIME  QUALITY"
		COMPREPLY=( $(compgen -W "${answers}" -- ${cur}) )
 		return 0
		;;
	--log-level|--console-log-level)
		answers="TRACE  DEBUG  INFO  WARN  ERROR  OFF"
		COMPREPLY=( $(compgen -W "${answers}" -- ${cur}) )
 		return 0
		;;
	--mask-censored-data-as-kappa-max|--mask-inactive-conditional-parameters-as-default-value|--repair-smac-invariant|--repair|--replace-seeds|--treat-censored-data-as-uncensored|--check-instances-exist|--skip-features|--ignore-features|--use-instances|--use-cpu-time-in-tunertime|--algo-deterministic|--deterministic|--abort-on-crash|--abort-on-first-run-crash|--bound-runs|--cache-runs|--cache-runs-debug|--cache-runs-strictly-increasing-observer|--call-observer-before-completion|--check-for-unclean-shutdown|--check-for-unique-runconfigs|--check-for-unique-runconfigs-exception|--check-result-order-consistent|--check-sat-consistency|--checkSATConsistency|--check-sat-consistency-exception|--exception-on-prepost-command|--exit-on-failure|--file-cache|--file-cache-crash-on-cache-miss|--file-cache-crash-on-miss|--filter-zero-cutoff-runs|--kill-run-exceeding-captime|--leak-memory|--log-requests-responses|--log-requests-responses-rc-only|--log-requests-responses-rc|--observer-walltime-if-no-runtime|--prepost-log-output|--skip-outstanding-eval-tae|--synchronize-observers|--tae-stop-processing-on-shutdown|--track-scheduled-runs|--transform-crashed-quality|--use-dynamic-cutoffs|--verify-sat|--verify-SAT|--verifySAT|--tae-transform|--tae-transform-valid-values-only|--rf-full-tree-bootstrap|--rf-ignore-conditionality|--rf-impute-mean|--rf-log-model|--log-model|--rf-penalize-imputed-values|--rf-preprocess-marginal|preprocessMarginal|--rf-shuffle-imputed-values|--rf-store-data|--rf-store-data-in-leaves|--rf-subsample-values-when-low-on-memory|--useBrokenVarianceCalculation)
		answers="true  false"
		COMPREPLY=( $(compgen -W "${answers}" -- ${cur}) )
 		return 0
		;;
	*)
		answers="--abort-on-crash --abort-on-first-run-crash --algo --algo-cutoff-length --algo-cutoff-time --algo-deterministic --algo-exec --algo-exec-dir --bound-runs --cache-runs --cache-runs-debug --cache-runs-strictly-increasing-observer --call-observer-before-completion --check-for-unclean-shutdown --check-for-unique-runconfigs --check-for-unique-runconfigs-exception --check-instances-exist --check-result-order-consistent --check-sat-consistency --check-sat-consistency-exception --checkSATConsistency --console-log-level --continous-neighbours --continuous-neighbors --cores --cputime-limit --cputime_limit --cutoff-time --cutoff_length --cutoff_time --deterministic --directories --exception-on-prepost-command --exec-dir --execdir --exit-on-failure --experiment-dir --feature-file --feature_file --file-cache --file-cache-crash-on-cache-miss --file-cache-crash-on-miss --file-cache-output --file-cache-source --filter-zero-cutoff-runs --fork-to-tae --fork-to-tae-duplicate-on-slave-quick-timeout --fork-to-tae-policy --help --help-default-file --help-level --ignore-features --imputation-iterations --instance-dir --instance-file --instance-regex --instance-suffix --instance_file --instance_seed_file --instances --inter-instance-obj --inter-obj --inter_instance_obj --intra-instance-obj --intra-obj --intra_instance_obj --invalid-scenario-reason --iteration-limit --kill-run-exceeding-captime --kill-run-exceeding-captime-factor --kill-runs-on-file-delete --leak-memory --leak-memory-amount --log-level --log-model --log-requests-responses --log-requests-responses-rc --log-requests-responses-rc-only --mask-censored-data-as-kappa-max --mask-inactive-conditional-parameters-as-default-value --max-norun-challenge-limit --num-trees --observer-walltime-delay --observer-walltime-if-no-runtime --observer-walltime-scale --outdir --output-dir --overall-obj --overall_obj --param-file --paramfile --pcs-file --post-scenario-command --post_cmd --pre-scenario-command --pre_cmd --prepost-exec-dir --prepost-log-output --repair --repair-smac-invariant --replace-seeds --restore-args --restore-scenario-arguments --retry-crashed-count --rf-full-tree-bootstrap --rf-ignore-conditionality --rf-impute-mean --rf-log-model --rf-min-variance --rf-num-trees --rf-penalize-imputed-values --rf-preprocess-marginal --rf-ratio-features --rf-shuffle-imputed-values --rf-split-min --rf-store-data --rf-store-data-in-leaves --rf-subsample-memory-percentage --rf-subsample-percentage --rf-subsample-values-when-low-on-memory --run-hashcode-file --run-obj --run-objective --run_obj --runcount-limit --runcount_limit --runtime-limit --scenario --scenario-file --search-subspace --search-subspace-file --seed --show-hidden --skip-features --skip-outstanding-eval-tae --split-min --synchronize-observers --tae --tae-default-file --tae-stop-processing-on-shutdown --tae-transform --tae-transform-SAT-quality --tae-transform-SAT-runtime --tae-transform-TIMEOUT-quality --tae-transform-TIMEOUT-runtime --tae-transform-UNSAT-quality --tae-transform-UNSAT-runtime --tae-transform-other-quality --tae-transform-other-runtime --tae-transform-valid-values-only --tae-warn-if-no-response-from-tae --target-run-cputime-limit --target_run_cputime_limit --terminate-on-delete --test-instance-dir --test-instance-file --test-instance-regex --test-instance-suffix --test-instances --test_instance_file --test_instance_seed_file --track-scheduled-runs --track-scheduled-runs-resolution --transform-crashed-quality --transform-crashed-quality-value --treat-censored-data-as-uncensored --tuner-timeout --tunertime-limit --up-to-iteration --up-to-tunertime --use-cpu-time-in-tunertime --use-dynamic-cutoffs --use-instances --useBrokenVarianceCalculation --verify-SAT --verify-sat --verifySAT --version --wallclock-limit --wallclock_limit "
		if [[ ${cur} == -* ]] ; then
        		COMPREPLY=( $(compgen -W "${answers}" -- ${cur}) )
	        	return 0
    		fi
		;;
	esac
}
complete -F _state_merge state-merge

