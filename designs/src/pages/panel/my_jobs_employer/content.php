<div class="content my-disputes-arbiter with-menu with-tabs">
    <?php include('../inc/lateral_menu.php'); ?>
    <div class="box">
        <div class="geral-buttons-opts-screen"> 
            <div class="flex">
                <a class="buttons-tabs-screen active"><span>Ongoing Jobs</span></a>
                <a class="buttons-tabs-screen"><span>Finished Jobs</span></a>
            </div>
            <div class="only-mobile combobox">
                <select>
                    <option value="0">Ongoing Jobs</option>
                    <option value="1">Finished Jobs</option>
                </select>
                <div class="items"></div>
            </div>
            <span class="icon-opt-title"><img src="./images/svg/ic-white-arrow-down.svg" alt=""></span>
        </div>
        <div class="box-content box-shadow min-height">
            <?php include ('./table.php'); ?>
            <?php include ('./table.php'); ?>
        </div>
    </div>
</div>