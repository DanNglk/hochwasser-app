#!/usr/bin/python
# -*- coding: utf8 -*-

import requests
import sys


def main(argv):
    baseURL = argv[0]

    # Add new data source to ODS
    payload = {'domainIdKey':'/gaugeId','schema':{'$schema':'http://json-schema.org/draft-04/schema#','type':'object','properties':{'gaugeId':{'type':'string'},'name':{'type':'string'},'km':{'type':'number'},'agency':{'type':'string'},'longitude':{'type':'number'},'latitude':{'type':'number'},'water':{'type':'string'},'currentMeasurement':{'type':'object','properties':{'timestamp':{'type':'string'},'value':{'type':'number'},'trend':{'type':'integer'},'stateMnwMhw':{'type':'string'},'stateNswHsw':{'type':'string'},'characteristicValues':{'type':'array','items':{'type':'object','properties':{'shortname':{'type':'string'},'longname':{'type':'string'},'unit':{'type':'string'},'value':{'type':'integer'},'validFrom':{'type':'string'},'timespanStart':{'type':'string'},'timespanEnd':{'type':'string'},'occurrences':{'type':'array','items':{'type':'string'}}}}}}}}},'metaData':{'name':'de-pegelonline','title':'pegelonline','author':'Wasser-undSchifffahrtsverwaltungdesBundes(WSV)','authorEmail':'https://www.pegelonline.wsv.de/adminmail','notes':'PEGELONLINEstelltkostenfreitagesaktuelleRohwerteverschiedenergewÃƒÂ¤sserkundlicherParameter(z.B.Wasserstand)derBinnen-undKÃƒÂ¼stenpegelderWasserstraÃƒÅ¸endesBundesbismaximal30TagerÃƒÂ¼ckwirkendzurAnsichtundzumDownloadbereit.','url':'https://www.pegelonline.wsv.de','termsOfUse':'http://www.pegelonline.wsv.de/gast/nutzungsbedingungen'}}
    result = requests.put(baseURL + '/datasources/pegelalarm', json=payload, auth=('admin@adminland.com', 'admin123'))
    print(result.text)

    # Add new processor chain to ODS
    payload = {'processors':[{'name':'JsonSourceAdapter','arguments':{'sourceUrl':'http://pegelonline.wsv.de/webservices/rest-api/v2/stations.json?includeTimeseries=true&includeCurrentMeasurement=true&includeCharacteristicValues=true'}},{'name':'PegelOnlineMerger','arguments':{}},{'name':'DbInsertionFilter','arguments':{'updateData':True}},{'name':'NotificationFilter','arguments':{}}],'executionInterval':{'period':1,'unit':'MINUTES'}}
    result = requests.put(baseURL + '/datasources/pegelalarm/filterChains/pegelonline', json=payload, auth=('admin@adminland.com', 'admin123'))
    print(result.text)

if __name__ == "__main__":
   main(sys.argv[1:])