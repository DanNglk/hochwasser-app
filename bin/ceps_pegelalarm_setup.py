#!/usr/bin/python
# -*- coding: utf8 -*-

import requests
import sys


def main(argv):
    baseURL = argv[0]

    # Add new data source to CEPS
    result = requests.put(baseURL + '/sources/pegelalarm', auth=('admin@adminland.com', 'admin123'))
    print(result.text)

    # Add epl adapter above level to CEPS
    payload = {'eplBlueprint':'selectstation1,station2frompattern[everystation1=`pegelalarm`(gaugeId=\'GAUGE_ID\')->station2=`pegelalarm`(gaugeId=\'GAUGE_ID\')]wherestation1.currentMeasurement.value<=LEVELandstation2.currentMeasurement.value>LEVEL','requiredArguments':{'GAUGE_ID':'STRING','LEVEL':'NUMBER'}}
    result = requests.put(baseURL + '/eplAdapter/pegelAlarmAboveLevel', json=payload, auth=('admin@adminland.com', 'admin123'))
    print(result.text)

    # Add epl adapter below level to CEPS
    payload = {'eplBlueprint':'selectstation1,station2frompattern[everystation1=`pegelalarm`(gaugeId=\'GAUGE_ID\')->station2=`pegelalarm`(gaugeId=\'GAUGE_ID\')]wherestation1.currentMeasurement.value>=LEVELandstation2.currentMeasurement.value<LEVEL','requiredArguments':{'GAUGE_ID':'STRING','LEVEL':'NUMBER'}}
    result = requests.put(baseURL + '/eplAdapter/pegelAlarmBelowLevel', json=payload, auth=('admin@adminland.com', 'admin123'))
    print(result.text)

    # Add epl adapter above or below level to CEPS
    payload = {'eplBlueprint':'selectstation1,station2frompattern[everystation1=`pegelalarm`(gaugeId=\'GAUGE_ID\')->station2=`pegelalarm`(gaugeId=\'GAUGE_ID\')]where(station1.currentMeasurement.value>=LEVELandstation2.currentMeasurement.value<LEVEL)or(station1.currentMeasurement.value<=LEVELandstation2.currentMeasurement.value>LEVEL)','requiredArguments':{'GAUGE_ID':'STRING','LEVEL':'NUMBER'}}
    result = requests.put(baseURL + '/eplAdapter/pegelAlarmAboveOrBelowLevel', json=payload, auth=('admin@adminland.com', 'admin123'))
    print(result.text)

if __name__ == "__main__":
   main(sys.argv[1:])