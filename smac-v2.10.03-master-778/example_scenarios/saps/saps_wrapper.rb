
#=== Deal with inputs.
if ARGV.length < 5
	puts "saps_wrapper.rb is a wrapper for the SAPS algorithm."
	puts "Usage: ruby saps_wrapper.rb <instance_relname> <instance_specifics> <cutoff_time> <cutoff_length> <seed> <params to be passed on>."
	exit -1
end
cnf_filename = ARGV[0]
instance_specifics = ARGV[1]
cutoff_time = ARGV[2].to_f
cutoff_length = ARGV[3].to_i
seed = ARGV[4].to_i

#=== Here I assume instance_specifics only contains the desired target quality or nothing at all for the instance, but it could contain more (to be specified in the instance_file or instance_seed_file)
if instance_specifics == ""
	qual = 0
else
	qual = instance_specifics.split[0]
end

paramstring = ARGV[5...ARGV.length].join(" ")

#=== Build algorithm command and execute it.

os = RUBY_PLATFORM


cmd = "#{File.dirname(__FILE__)}/ubcsat -alg saps #{paramstring} -inst #{cnf_filename} -cutoff #{cutoff_length} -timeout #{cutoff_time} -target #{qual} -seed #{seed} -r stats stdout default,best"

if os.include? "mac" or os.include? "darwin"
  puts "Mac OS X Detected"
  cmd = "#{File.dirname(__FILE__)}/ubcsat-mac -alg saps #{paramstring} -inst #{cnf_filename} -cutoff #{cutoff_length} -timeout #{cutoff_time} -target #{qual} -seed #{seed} -r stats stdout default,best"
end

if os.include?"win" or os.include?"msys" or os.include?"mingw" or os.include?"emc"
  puts "Windows Detected"
  cmd = "#{File.absolute_path(File.dirname(__FILE__))}\\ubcsat.exe -alg saps #{paramstring} -inst #{cnf_filename} -cutoff #{cutoff_length} -timeout #{cutoff_time} -target #{qual} -seed #{seed} -r stats stdout default,best"
end

filename = "ubcsat_output#{rand}.txt"
exec_cmd = "#{cmd} > #{filename}"

puts "Calling: #{exec_cmd}"
system exec_cmd

#=== Parse algorithm output to extract relevant information for ParamILS.
solved = "CRASHED"
runtime = cutoff_time
runlength = 0
best_sol = 0

File.open(filename){|file|
	while line = file.gets
		if line =~ /SuccessfulRuns = (\d+)/
			numsolved = $1.to_i
			if numsolved > 0
				solved = "SUCCESS"
			else
				solved = "TIMEOUT"
			end
		end
		if line =~ /CPUTime_Mean = (.*)$/
			runtime = $1.to_f
		end
		if line =~ /Steps_Mean = (\d+)/
			runlength = $1.to_i
		end
		if line =~ /BestSolution_Mean = (\d+)/
			best_sol = $1.to_i
		end
	end
}
File.delete(filename)
puts "Result of algorithm run: #{solved}, #{runtime}, #{runlength}, #{best_sol}, #{seed}"
