import logging
import json
import os 
from azure.cosmos import exceptions, CosmosClient, PartitionKey
import azure.functions as func
import uuid
import sys

def main(req: func.HttpRequest) -> func.HttpResponse:
    logging.info('Python HTTP trigger function processed a request.')

    work = req.params.get('func')
    if not work:
        try:
            req_body = req.get_json()
        except ValueError:
            pass
        else:
            name = req_body.get('func')

    if 'get' in work:
        return func.HttpResponse(getData())
    if 'append' in work:
        lat = req.params.get('lat')
        if not lat:
            try:
                req_body = req.get_json()
            except ValueError:
                pass
            else:
                lat = req_body.get('lat')
        lon=req.params.get('lon')
        if not lon:
            try:
                req_body = req.get_json()
            except ValueError:
                pass
            else:
                lat = req_body.get('lat')
        return func.HttpResponse(
             updateData(lat,lon),
             status_code=200
        )

def getData():
    url = 'https://waterdb.documents.azure.com:443/'
    key = '[REDACTED]'
    client = CosmosClient(url, credential=key)
    database_name = 'SourceLoc'
    database = client.get_database_client(database_name)
    container_name = 'LocationDB'
    container = database.get_container_client(container_name)

    # Enumerate the returned items

    s=''
    for item in container.query_items(
        query='SELECT * FROM c',
        enable_cross_partition_query=True):
            s=s+(json.dumps(item, indent=True))
    return s

def updateData(lat, lon):
   

    endpoint = 'https://waterdb.documents.azure.com:443/'
    key = '[REDACTED]'

    client = CosmosClient(endpoint, key)
    database_name = 'SourceLoc'
    database = client.create_database_if_not_exists(id=database_name)
    container_name = 'LocationDB'
    container = database.create_container_if_not_exists(
        id=container_name,
        partition_key=PartitionKey(path="/loc"),
        offer_throughput=400
    )
    user_items_to_create = [{
            'lat':lat,
            'lon':lon,
            'id':str(uuid.uuid4())
        }]
    for user_item in user_items_to_create:
        container.create_item(body=user_item)

    return 'Updated'