
# spark masters
server 'ld4p_dev_spark_master', user: 'root', roles: %w{redhat spark master}

# spark workers
server 'ld4p_dev_spark_worker1', user: 'root', roles: %w{redhat spark worker}
server 'ld4p_dev_spark_worker2', user: 'root', roles: %w{redhat spark worker}
server 'ld4p_dev_spark_worker3', user: 'root', roles: %w{redhat spark worker}

# ----
# Setup the environment variables for the spark app

set :ld4p_data, File.join(fetch(:deploy_to), 'current', 'src', 'main', 'resources', 'xsl')
set :bootstrap_servers, 'ec2-34-213-81-65.us-west-2.compute.amazonaws.com:9092,ec2-34-214-42-7.us-west-2.compute.amazonaws.com:9092,ec2-52-36-184-167.us-west-2.compute.amazonaws.com:9092'

set :default_env, {
  LD4P_DATA: fetch(:ld4p_data),
  BOOTSTRAP_SERVERS: fetch(:bootstrap_servers)
}

