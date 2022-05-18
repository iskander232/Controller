#!/bin/bash

required_args_num=0
admin_api_args=0
while [ $# -gt 0 ]; do
  case "$1" in
    --cluster-id=*)
      cluster_id="${1#*=}"
      required_args_num=$((required_args_num + 1))
      ;;
    --node-id=*)
      node_id="${1#*=}"
      required_args_num=$((required_args_num + 1))
      ;;
    --controller-port=*)
      controller_port="${1#*=}"
      required_args_num=$((required_args_num + 1))
      ;;
    --controller-address=*)
      controller_address="${1#*=}"
      required_args_num=$((required_args_num + 1))
      ;;
    --admin-api-port=*)
      admin_api_port="${1#*=}"
      admin_api_args=$((admin_api_args + 1))
      ;;
    --admin-api-address=*)
      admin_api_address="${1#*=}"
      admin_api_args=$((admin_api_args + 1))
      ;;
    *)
      printf "***************************\n"
      printf "* Error: Invalid argument.*\n"
      printf "***************************\n"
      exit 1
  esac
  shift
done

if [[ "$required_args_num" != "4" ]]; then
    echo "required_args_num=$required_args_num"
    echo "required args are --cluster-id --node-id --controller-port --controller-address"
    exit 1
fi


echo "node:
  cluster: $cluster_id
  id: $node_id

dynamic_resources:
  ads_config:
    api_type: GRPC
    transport_api_version: V3
    grpc_services:
    - envoy_grpc:
        cluster_name: xds_cluster
  cds_config:
    resource_api_version: V3
    ads: {}
  lds_config:
    resource_api_version: V3
    ads: {}

static_resources:
  clusters:
  - type: STRICT_DNS
    typed_extension_protocol_options:
      envoy.extensions.upstreams.http.v3.HttpProtocolOptions:
        \"@type\": type.googleapis.com/envoy.extensions.upstreams.http.v3.HttpProtocolOptions
        explicit_http_config:
          http2_protocol_options: {}
    name: xds_cluster
    load_assignment:
      cluster_name: xds_cluster
      endpoints:
      - lb_endpoints:
        - endpoint:
            address:
              socket_address:
                address: $controller_address
                port_value: $controller_port"

if [[ "$admin_api_args" == "2" ]]; then
    echo "admin:
  address:
    socket_address:
      address: $admin_api_address
      port_value: $admin_api_port"
fi
