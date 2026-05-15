#!/usr/bin/env python

import pandas as pd
import numpy as np
import json
import textwrap
import argparse

def prepare_data(df_):
    df_ = df_.rename({'pValue': 'p_value', 'fdr': 'FDR'}, axis='columns')
    df_['FDR'] = -np.log(df_['FDR'])
    df_['p_value'] = -np.log(df_['p_value'])
    df_['GO_label'] = df_['term'].apply(lambda x: "<br>".join(textwrap.wrap(x['label'], 50)) if 'label' in x.keys() else None)
    df_['GO_id'] = df_['term'].apply(lambda x: "<br>".join(textwrap.wrap(x['id'], 50)) if 'id' in x.keys() else None)
    df_ = df_.dropna()
    return df_

def get_significance_level(df_, sort_by_, color_by_, max_entries_cutoff_, FDR_cutoff_, p_value_cutoff_, output_file_): 
    df_filtered = df_
    cutoff_value = -np.log(FDR_cutoff_) if sort_by_ == "FDR" else -np.log(p_value_cutoff_) if sort_by_ == "p_value" else -1.0
    df_filtered['significance'] = df_filtered[sort_by_] >= cutoff_value if cutoff_value != -1.0 else True
    df_filtered = df_filtered.sort_values(by=[sort_by_], ascending=False)
    
    if df_filtered.loc[df_filtered['significance'] == True].shape[0] > 0:
        df_filtered = df_filtered.head(min(df_filtered.loc[df_filtered['significance'] == True].shape[0], max_entries_cutoff_))
    else: 
        df_filtered = df_filtered.head(max_entries_cutoff_) 
        
    pd.DataFrame({'x': df_filtered[sort_by_], 
                  'y': df_filtered['GO_label'], 
                  'color': df_filtered[color_by_]}).to_json(output_file_, orient='columns')
 

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Interactive GO plot')
    parser.add_argument('--input_file', help="Path to the GO analysis result in json format", type=str)
    parser.add_argument('--output_file', help="Path to the GO analysis plot in html format", type=str)
    parser.add_argument('--GO_layout', help="Path to the GO layout json file", type=str)
    parser.add_argument('--sort_by', help="Column to sort GO terms. Options: FDR, p_value, fold_enrichment",
                        default='p_value', type=str)
    parser.add_argument('--color_by', help="Column to color GO terms. Options: FDR, p_value, fold_enrichment",
                        default='fold_enrichment', type=str)
    parser.add_argument('--sig_cutoff', help="Threshold for significance level if sort_by is FDR or p_value.",
                        default=0.5, type=float)
    parser.add_argument('--max_entries_cutoff', help="Number of maximum entries to display on the plot",
                        default=25, type=int)
    args = parser.parse_args()
    
    input_file = args.input_file 
    output_file = args.output_file 
    GO_layout = args.GO_layout 
    sort_by = args.sort_by
    color_by = args.color_by
    sig_cutoff = args.sig_cutoff
    max_entries_cutoff = args.max_entries_cutoff
    
    with open(input_file, 'r') as f:
        data = json.load(f)
        df = pd.DataFrame(data['results']['result'])
        df = prepare_data(df)
        
    with open(GO_layout, 'r') as f:
        main_layout = json.load(f)
        
    labels = {'FDR': '-Log(FDR)', 'p_value': '-Log(p-value)', 'fold_enrichment': 'Fold Enrichment'}
    get_significance_level(df_=df, sort_by_=sort_by, color_by_=color_by, max_entries_cutoff_=max_entries_cutoff, 
                                   FDR_cutoff_=sig_cutoff, p_value_cutoff_=sig_cutoff, output_file_=output_file)