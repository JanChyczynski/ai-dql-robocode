import os
import glob
import shutil
import sys
import pandas as pd
import matplotlib.pyplot as plt
from datetime import datetime

# CONSTANT: Path to Robocode working directory on Windows
WORKING_DIR = r"C:\\robocode\\robots\\forkbot\\ForkBot.data"  # correct now

# === Helper Functions ===
def moving_average(series, window):
    return series.rolling(window=window, min_periods=1).mean()

def create_plots(df, plot_dir, subtitle):
    os.makedirs(plot_dir, exist_ok=True)

    # Compute moving average
    window = max(1, int(len(df) * 0.05))
    df_ma = df.rolling(window=window).mean()

    metrics = ["reward", "damage", "damageTaken", "win", "wallHits"]

    for metric in metrics:
        plt.figure()
        plt.plot(df_ma[metric])
        plt.title(f"{metric.capitalize()} over Rounds")
        plt.suptitle(subtitle, fontsize=10, y=0.94)
        plt.xlabel("Round")
        plt.ylabel(metric.capitalize())
        plt.grid(True)
        plt.tight_layout()
        plt.savefig(os.path.join(plot_dir, f"{metric}.png"))
        plt.close()

    # Combined plot
    plt.figure()
    for metric in metrics:
        plt.plot(df_ma[metric], label=metric)
    plt.title("Combined Metrics over Rounds")
    plt.suptitle(subtitle, fontsize=10, y=0.94)
    plt.xlabel("Round")
    plt.ylabel("Value")
    plt.legend()
    plt.grid(True)
    plt.tight_layout()
    plt.savefig(os.path.join(plot_dir, "combined.png"))
    plt.close()

# === Main Logic ===
def process_log(rename_after_processing):
    os.chdir(WORKING_DIR)
    log_files = glob.glob("unprocessed_*.log")
    if not log_files:
        print("No unprocessed log files found.")
        return

    log_file = log_files[0]

    # === Header Fix ===
    with open(log_file, 'r+') as f:
        first_line = f.readline()
        if "reward" not in first_line:
            content = f.read()
            f.seek(0)
            f.write("reward,damage,damageTaken,win,wallHits\n" + first_line + content)
            print("Header added to log file.")

    df = pd.read_csv(log_file)

    # Extract typeName and parameters from the filename
    base_name = os.path.splitext(log_file)[0]
    parts = base_name.split('_', 2)
    type_name = parts[1]
    parameters = parts[2]

    # Create timestamp
    timestamp = datetime.now().strftime("%Y-%m-%d_%H-%M")

    # Set output paths
    plot_dir = f"plots_{timestamp}_{type_name}_{parameters}"
    done_log = f"done_{timestamp}_{type_name}_{parameters}.log"

    # Create plots
    create_plots(df, plot_dir, f"{timestamp}_{type_name}_{parameters}")

    # Remove the header-written flag file
    header_tmp = log_file + ".header_written.tmp"
    if os.path.exists(header_tmp):
        os.remove(header_tmp)
        print(f"Removed header flag: {header_tmp}")

    # Rename only if requested
    if rename_after_processing:
        shutil.move(log_file, done_log)
        print(f"Renamed log to: {done_log}")
    else:
        print(f"Log file left as: {log_file}")

    print(f"Processed: {log_file}")
    print(f"Saved plots to: {plot_dir}")

# === Script Entry ===
if __name__ == "__main__":
    rename = False
    if len(sys.argv) > 1 and sys.argv[1] == '1':
        rename = True
    process_log(rename)
