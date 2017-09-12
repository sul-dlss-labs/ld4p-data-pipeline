namespace :spark do

  after :deploy, 'spark:update_env'

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

end
