import neurokit2 as nk
import pandas as pd
from pyPPG import PPG, Fiducials, Biomarkers
# from pyPPG.datahandling import load_data, plot_fiducials, save_data
import pyPPG.preproc as PP
import pyPPG.fiducials as FP
import pyPPG.biomarkers as BM
from dotmap import DotMap
from sklearn.svm import SVC
import numpy as np
from joblib import dump, load
import os
import pyPPG.ppg_sqi as SQI
from com.chaquo.python import Python


def extract_feature(file_path):
    ppg = extract_ppg(file_path)
    cleaned_ppg = nk.ppg_clean(ppg)
    signal = DotMap()
    signal.v = cleaned_ppg
    signal.fs = 256
    signal.filtering = True # whether or not to filter the PPG signal
    signal.fL=0.5000001 # Lower cutoff frequency (Hz)
    signal.fH=12 # Upper cutoff frequency (Hz)
    signal.order=4 # Filter order
    signal.sm_wins={'ppg':50,'vpg':10,'apg':10,'jpg':10} # smoothing windows in millisecond for the PPG, PPG', PPG", and PPG'"

    prep = PP.Preprocess(fL=signal.fL, fH=signal.fH, order=signal.order, sm_wins=signal.sm_wins)
    signal.ppg, signal.vpg, signal.apg, signal.jpg = prep.get_signals(s=signal)

    corr_on = ['on', 'dn', 'dp', 'v', 'w', 'f']
    correction=pd.DataFrame()
    correction.loc[0, corr_on] = True
    signal.correction=correction

    s = PPG(signal)

    fpex = FP.FpCollection(s=s)

    fiducials = fpex.get_fiducials(s=s)

    fp = Fiducials(fp=fiducials)

    bmex = BM.BmCollection(s=s, fp=fp)

    bm_defs, bm_vals, bm_stats = bmex.get_biomarkers()

    bm = Biomarkers(bm_defs=bm_defs, bm_vals=bm_vals, bm_stats=bm_stats)
    all_stats = []

    for key, df in bm.bm_stats.items():
        all_stats.append(df)

    all_stats_df = pd.concat(all_stats, axis=1)

    return list(all_stats_df.loc['mean'])

def store_model(writable_dir, original_list):
    X_train = np.empty((50, 102))
    for i in range(50):
        np.random.shuffle(original_list)
        X_train[i] = original_list.copy()

    Y_train = np.random.randint(2, size=50)

    svm_classifier = SVC(kernel='rbf', random_state=42, probability=True)
    svm_classifier.fit(X_train, Y_train)

    os.makedirs(writable_dir, exist_ok=True)
    file_path = os.path.join(writable_dir, 'test.joblib')

    dump(svm_classifier, file_path)


def inference(file_path):
    X_test = np.random.uniform(-10, 10, (1, 102))

    model = load(file_path, mmap_mode='r')

    result = model.predict(X_test)

    return result[0]

def extract_ppg(file_path):
    df = pd.read_csv(file_path)
    first_row = df.iloc[0]
    second_row = df.iloc[1]
    matching_columns = df.columns[(first_row == "PPG_A13") & (second_row == "CAL")]
    result = df[matching_columns][4:].to_numpy().flatten().astype(float)

    return result

# if __name__ == "__main__":
#     # Log the current working directory
#     logger = Python.getPlatform().getLogger()
#     current_working_directory = os.getcwd()
#     logger.info(f"Current working directory: {current_working_directory}")