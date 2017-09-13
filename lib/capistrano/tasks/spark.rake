namespace :spark do

  after :deploy, 'spark:update_env'
  after :deploy, 'spark:assembly'
  after :deploy, 'spark:upload_assembly'

  # Set the /etc/environment
  desc 'Update the spark environment variables'
  task :update_env do
    on roles(:spark) do
      # remove any existing entries
      sudo("sed -i -e '/BEGIN_LD4P_ENV/,/END_LD4P_ENV/{ d; }' /etc/environment")
      # append new entries
      sudo("echo '### BEGIN_LD4P_ENV' | sudo tee -a /etc/environment > /dev/null")
      sudo("echo 'export LD4P_DATA=#{fetch(:ld4p_data)}' | sudo tee -a /etc/environment > /dev/null")
      sudo("echo 'export BOOTSTRAP_SERVERS=#{fetch(:bootstrap_servers)}' | sudo tee -a /etc/environment > /dev/null")
      sudo("echo '### END_LD4P_ENV' | sudo tee -a /etc/environment > /dev/null")
    end
  end

  # ----
  # Build and deploy the spark projects

  desc 'sbt SparkStreamingConvertors/assembly'
  task :assembly do
    system('sbt SparkStreamingConvertors/assembly')
  end

  desc 'Upload the spark package'
  task :upload_assembly do
    jars = Dir.glob('./SparkStreamingConvertors/**/*assembly*.jar')
    on roles(:spark) do
      lib_path = File.join(current_path, 'lib')
      execute("mkdir -p #{lib_path}")
      jars.each do |jar|
        filename = File.basename(jar)
        lib = File.join(lib_path, filename)
        # puts "#{jar} -> #{lib}"
        upload!(jar, lib)
      end
    end
  end
end
