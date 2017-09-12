# server-based syntax
# ======================
# Defines a single server with a list of roles and multiple properties.
# You can define all roles on a single server, or split them:

# server "example.com", user: "deploy", roles: %w{app db web}, my_property: :my_value
# server "example.com", user: "deploy", roles: %w{app web}, other_property: :other_value
# server "db.example.com", user: "deploy", roles: %w{db}

# spark masters
server "ld4p_dev_spark_master", user: "root", roles: %w{redhat spark master}

# spark slaves
server "ld4p_dev_spark_slave1", user: "root", roles: %w{redhat spark slave}
server "ld4p_dev_spark_slave2", user: "root", roles: %w{redhat spark slave}
server "ld4p_dev_spark_slave3", user: "root", roles: %w{redhat spark slave}


# Setup the environment variables for the spark app

set :ld4p_data, File.join(fetch(:deploy_to), 'current', 'src', 'main', 'resources', 'xsl')
set :bootstrap_servers, "ec2-34-213-81-65.us-west-2.compute.amazonaws.com:9092,ec2-34-214-42-7.us-west-2.compute.amazonaws.com:9092,ec2-52-36-184-167.us-west-2.compute.amazonaws.com:9092"

set :default_env, {
  LD4P_DATA: fetch(:ld4p_data),
  BOOTSTRAP_SERVERS: fetch(:bootstrap_servers)
}

# Set the /etc/environment
def spark_environment
  # remove any existing entries
  sudo("sed -i -e '/BEGIN_LD4P_ENV/,/END_LD4P_ENV/{ d; }' /etc/environment")
  # append new entries
  sudo("echo '### BEGIN_LD4P_ENV' | sudo tee -a /etc/environment > /dev/null")
  sudo("echo 'export LD4P_DATA=#{fetch(:ld4p_data)}' | sudo tee -a /etc/environment > /dev/null")
  sudo("echo 'export BOOTSTRAP_SERVERS=#{fetch(:bootstrap_servers)}' | sudo tee -a /etc/environment > /dev/null")
  sudo("echo '### END_LD4P_ENV' | sudo tee -a /etc/environment > /dev/null")
end

after :deploy, :update_env do
  on roles(:spark) do
    spark_environment
  end
end



# role-based syntax
# ==================

# Defines a role with one or multiple servers. The primary server in each
# group is considered to be the first unless any hosts have the primary
# property set. Specify the username and a domain or IP for the server.
# Don't use `:all`, it's a meta role.

# role :app, %w{deploy@example.com}, my_property: :my_value
# role :web, %w{user1@primary.com user2@additional.com}, other_property: :other_value
# role :db,  %w{deploy@example.com}



# Configuration
# =============
# You can set any configuration variable like in config/deploy.rb
# These variables are then only loaded and set in this stage.
# For available Capistrano configuration variables see the documentation page.
# http://capistranorb.com/documentation/getting-started/configuration/
# Feel free to add new variables to customise your setup.



# Custom SSH Options
# ==================
# You may pass any option but keep in mind that net/ssh understands a
# limited set of options, consult the Net::SSH documentation.
# http://net-ssh.github.io/net-ssh/classes/Net/SSH.html#method-c-start
#
# Global options
# --------------
#  set :ssh_options, {
#    keys: %w(/home/rlisowski/.ssh/id_rsa),
#    forward_agent: false,
#    auth_methods: %w(password)
#  }
#
# The server-based syntax can be used to override options:
# ------------------------------------
# server "example.com",
#   user: "user_name",
#   roles: %w{web app},
#   ssh_options: {
#     user: "user_name", # overrides user setting above
#     keys: %w(/home/user_name/.ssh/id_rsa),
#     forward_agent: false,
#     auth_methods: %w(publickey password)
#     # password: "please use keys"
#   }

