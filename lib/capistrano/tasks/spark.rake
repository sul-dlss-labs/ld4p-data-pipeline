namespace :spark do

  after :deploy, 'spark:update_env'

  desc 'Update the spark environment variables'
  task :update_env do
    on roles(:spark) do
      # remove any existing entries
      sudo("sed -i -e '/BEGIN_LD4P_ENV/,/END_LD4P_ENV/{ d; }' /etc/environment")
      # append new entries
      execute("echo '### BEGIN_LD4P_ENV' | sudo tee -a /etc/environment > /dev/null")
      execute("echo 'export LD4P_DATA=#{fetch(:ld4p_data)}' | sudo tee -a /etc/environment > /dev/null")
      execute("echo 'export BOOTSTRAP_SERVERS=#{fetch(:bootstrap_servers)}' | sudo tee -a /etc/environment > /dev/null")
      execute("echo '### END_LD4P_ENV' | sudo tee -a /etc/environment > /dev/null")
    end
  end

  desc 'sbt SparkStreamingConvertors/assembly'
  task :assembly do
    on roles(:spark) do
      sudo("#{current_path}/lib/bash/redhat/sbt.sh")
      execute("cd #{current_path}; sbt SparkStreamingConvertors/assembly")
    end
  end

end
