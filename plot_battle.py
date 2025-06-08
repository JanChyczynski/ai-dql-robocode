import os
import pandas as pd
import matplotlib.pyplot as plt
import shutil
from datetime import datetime

# CONSTANT: Path to Robocode working directory on Windows
ROBOCODE_DATA_DIR = r"C:\\robocode\\robots\\.data\\mybot\\MyRobot.data"  # correct now

def main():
    # Step 1: Find unprocessed log file
    files = [f for f in os.listdir(ROBOCODE_DATA_DIR) if f.startswith("unprocessed_") and f.endswith(".log")]
    if not files:
        print("No unprocessed log files found.")
        return

    log_file = files[0]
    log_path = os.path.join(ROBOCODE_DATA_DIR, log_file)

    # Get current timestamp in required format
    timestamp = datetime.now().strftime("%Y-%m-%d_%H-%M")

    # Step 2: Derive typeName and parameters parts
    name_part = log_file.replace("unprocessed_", "").replace(".log", "")

    # Step 3: Create plots directory with timestamp
    plots_dir = os.path.join(ROBOCODE_DATA_DIR, f"plots_{timestamp}_{name_part}")
    os.makedirs(plots_dir, exist_ok=True)

    # Step 4: Read the CSV log data
    df = pd.read_csv(log_path)

    # Step 5: Plot individual metrics
    for column in df.columns:
        plt.figure()
        plt.plot(df[column])
        plt.title(column)
        plt.xlabel("Round")
        plt.ylabel(column)
        plt.grid(True)
        plt.savefig(os.path.join(plots_dir, f"{column}.png"))
        plt.close()

    # Step 6: Combined plot (excluding 'win' since it’s categorical)
    plt.figure()
    for column in df.columns:
        if column != 'win':
            plt.plot(df[column], label=column)
    plt.title("Combined Metrics (excluding 'win')")
    plt.xlabel("Round")
    plt.ylabel("Value")
    plt.legend()
    plt.grid(True)
    plt.savefig(os.path.join(plots_dir, "combined.png"))
    plt.close()

    # Step 7: Rename the log file with timestamp prefix
    done_file = f"done_{timestamp}_{name_part}.log"
    os.rename(log_path, os.path.join(ROBOCODE_DATA_DIR, done_file))
    print(f"Processed and renamed {log_file} -> {done_file}")

if __name__ == "__main__":
    main()
